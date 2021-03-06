/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2016 Vector Creations Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.olm;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.annotation.meta.When.MAYBE;
import static org.matrix.olm.OlmException.*;

import java.io.*;
import java.util.*;

import javax.annotation.*;

import com.beust.klaxon.*;
import org.slf4j.*;


/**
 * Account class used to create Olm sessions in conjunction with {@link OlmSession} class.<br>
 * OlmAccount provides APIs to retrieve the Olm keys.
 * <br><br>Detailed implementation guide is available at <a href="http://matrix.org/docs/guides/e2e_implementation.html">Implementing End-to-End Encryption in Matrix clients</a>.
 */
public class OlmAccount extends CommonSerializeUtils implements Serializable
{
	/**
	 * As well as the identity key, each device creates a number of Curve25519 key pairs which are
	 * also used to establish Olm sessions, but can only be used once. Once again, the private part
	 * remains on the device. but the public part is published to the Matrix network
	 **/
	@Nonnull
	public static final String JSON_KEY_ONE_TIME_KEY = "curve25519";
	/**
	 * Curve25519 identity key is a public-key cryptographic system which can be used to establish a shared
	 * secret.<br>In Matrix, each device has a long-lived Curve25519 identity key which is used to establish
	 * Olm sessions with that device. The private key should never leave the device, but the
	 * public part is signed with the Ed25519 fingerprint key ({@link #JSON_KEY_FINGER_PRINT_KEY}) and published to the network.
	 **/
	@Nonnull
	public static final String JSON_KEY_IDENTITY_KEY = "curve25519";
	
	// JSON keys used in the JSON objects returned by JNI
	/**
	 * Ed25519 finger print is a public-key cryptographic system for signing messages.<br>In Matrix, each device has
	 * an Ed25519 key pair which serves to identify that device. The private the key should
	 * never leave the device, but the public part is published to the Matrix network.
	 **/
	@Nonnull
	public static final String JSON_KEY_FINGER_PRINT_KEY = "ed25519";
	
	private static final long serialVersionUID = 3497486121598434824L;
	private static final Logger LOGGER = LoggerFactory.getLogger(OlmAccount.class);
	
	/**
	 * This class stores the identity keys (ed25519 and curve25519) of an account.
	 */
	public static class IdentityKeys
	{
		@Nullable private JsonObject json;
		@Nullable private String ed25519;
		@Nullable private String curve25519;
		
		public IdentityKeys(@Nonnull JsonObject identityKeysJson)
		{
			this.json = identityKeysJson;
		}
		
		public IdentityKeys(@Nonnull String ed25519, @Nonnull String curve25519)
		{
			this.ed25519 = ed25519;
			this.curve25519 = curve25519;
		}
		
		@Nonnull
		public JsonObject getJson()
		{
			if (json == null)
			{
				Map<String, Object> keys = new HashMap<>();
				keys.put(JSON_KEY_FINGER_PRINT_KEY, ed25519);
				keys.put(JSON_KEY_IDENTITY_KEY, curve25519);
				json = new JsonObject(keys);
			}
			return json;
		}
		
		@Nonnull
		public String getEd25519()
		{
			if (ed25519 == null)
				ed25519 = (String) json.get(JSON_KEY_FINGER_PRINT_KEY);
			return ed25519;
		}
		
		@Nonnull
		public String getCurve25519()
		{
			if (curve25519 == null)
				curve25519 = (String) json.get(JSON_KEY_IDENTITY_KEY);
			return curve25519;
		}
	}
	
	/**
	 * Account Id returned by JNI.
	 * This value identifies uniquely the native account instance.
	 */
	private transient long mNativeId;
	
	public OlmAccount()
			throws OlmException
	{
		try
		{
			mNativeId = createNewAccountJni();
		}
		catch (Exception e)
		{
			throw new OlmException(EXCEPTION_CODE_INIT_ACCOUNT_CREATION, e.getMessage());
		}
	}
	
	/**
	 * Create a new account and return it to JAVA side.<br>
	 * Since a C prt is returned as a jlong, special care will be taken
	 * to make the cast (OlmAccount* to jlong) platform independent.
	 *
	 * @return the initialized OlmAccount* instance or throw an exception if fails
	 **/
	private native long createNewAccountJni();
	
	/**
	 * Getter on the account ID.
	 *
	 * @return native account ID
	 */
	long getOlmAccountId()
	{
		return mNativeId;
	}
	
	/**
	 * Release native account and invalid its JAVA reference counter part.<br>
	 * Public API for {@link #releaseAccountJni()}.
	 */
	public void releaseAccount()
	{
		if (0 != mNativeId)
		{
			releaseAccountJni();
		}
		mNativeId = 0;
	}
	
	/**
	 * Destroy the corresponding OLM account native object.<br>
	 * This method must ALWAYS be called when this JAVA instance
	 * is destroyed (ie. garbage collected) to prevent memory leak in native side.
	 * See {@link #createNewAccountJni()}.
	 */
	private native void releaseAccountJni();
	
	/**
	 * Return true the object resources have been released.<br>
	 *
	 * @return true the object resources have been released
	 */
	public boolean isReleased()
	{
		return (0 == mNativeId);
	}
	
	/**
	 * Return the identity keys (identity and fingerprint keys).<br>
	 * Public API for {@link #identityKeysJni()}.
	 *
	 * @return identity keys
	 * @throws OlmException the failure reason
	 * @see #identityKeysJson()
	 */
	@Nonnull
	public IdentityKeys identityKeys()
			throws OlmException
	{
		return new IdentityKeys(identityKeysJson());
	}
	
	/**
	 * Return the identity keys (identity and fingerprint keys) in a json object.<br>
	 * Public API for {@link #identityKeysJni()}.<br>
	 * Ex:<tt>
	 * {
	 * "curve25519":"Vam++zZPMqDQM6ANKpO/uAl5ViJSHxV9hd+b0/fwRAg",
	 * "ed25519":"+v8SOlOASFTMrX3MCKBM4iVnYoZ+JIjpNt1fi8Z9O2I"
	 * }</tt>
	 *
	 * @return identity keys json if operation succeeds
	 * @throws OlmException the failure reason
	 */
	@Nonnull
	public JsonObject identityKeysJson()
			throws OlmException
	{
		byte[] identityKeysBuffer;
		
		try
		{
			identityKeysBuffer = identityKeysJni();
		}
		catch (Exception e)
		{
			LOGGER.error("## identityKeys(): Failure - " + e.getMessage());
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_IDENTITY_KEYS, e.getMessage());
		}
		
		if (null != identityKeysBuffer)
		{
			JsonObject identityKeysJson = (JsonObject) new Parser().parse(new ByteArrayInputStream(identityKeysBuffer), UTF_8);
			if (identityKeysJson == null)
				throw new OlmException(EXCEPTION_CODE_ACCOUNT_IDENTITY_KEYS, "failed to parse json");
			return identityKeysJson;
		}
		else
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_IDENTITY_KEYS, "identityKeysJni()=null");
	}
	
	/**
	 * Get the public identity keys (Ed25519 fingerprint key and Curve25519 identity key).<br>
	 * Keys are Base64 encoded.
	 * These keys must be published on the server.
	 *
	 * @return the identity keys or throw an exception if it fails
	 */
	private native byte[] identityKeysJni();
	
	/**
	 * Return the largest number of "one time keys" this account can store.
	 *
	 * @return the max number of "one time keys", -1 otherwise
	 */
	public long maxOneTimeKeys()
	{
		return maxOneTimeKeysJni();
	}
	
	/**
	 * Return the largest number of "one time keys" this account can store.
	 *
	 * @return the max number of "one time keys", -1 otherwise
	 */
	private native long maxOneTimeKeysJni();
	
	/**
	 * Generate a number of new one time keys.<br> If total number of keys stored
	 * by this account exceeds {@link #maxOneTimeKeys()}, the old keys are discarded.<br>
	 * The corresponding keys are retrieved by {@link #oneTimeKeys()}.
	 *
	 * @param aNumberOfKeys number of keys to generate
	 * @throws OlmException the failure reason
	 */
	public void generateOneTimeKeys(int aNumberOfKeys)
			throws OlmException
	{
		try
		{
			generateOneTimeKeysJni(aNumberOfKeys);
		}
		catch (Exception e)
		{
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_GENERATE_ONE_TIME_KEYS, e.getMessage());
		}
	}
	
	/**
	 * Generate a number of new one time keys.<br> If total number of keys stored
	 * by this account exceeds {@link #maxOneTimeKeys()}, the old keys are discarded.
	 * An exception is thrown if the operation fails.<br>
	 *
	 * @param aNumberOfKeys number of keys to generate
	 */
	private native void generateOneTimeKeysJni(int aNumberOfKeys);
	
	/**
	 * Return the "one time keys" in a dictionary.<br>
	 * The number of "one time keys", is specified by {@link #generateOneTimeKeys(int)}<br>
	 * Ex:<tt>
	 * { "curve25519":
	 * {
	 * "AAAABQ":"qefVZd8qvjOpsFzoKSAdfUnJVkIreyxWFlipCHjSQQg",
	 * "AAAABA":"/X8szMU+p+lsTnr56wKjaLgjTMQQkCk8EIWEAilZtQ8",
	 * "AAAAAw":"qxNxxFHzevFntaaPdT0fhhO7tc7pco4+xB/5VRG81hA",
	 * }
	 * }</tt><br>
	 * Public API for {@link #oneTimeKeysJni()}.<br>
	 * Note: these keys are to be published on the server.
	 *
	 * @return one time keys in string dictionary.
	 * @throws OlmException the failure reason
	 */
	@Nonnull
	public JsonObject oneTimeKeys()
			throws OlmException
	{
		byte[] oneTimeKeysBuffer;
		
		try
		{
			oneTimeKeysBuffer = oneTimeKeysJni();
		}
		catch (Exception e)
		{
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_ONE_TIME_KEYS, e.getMessage());
		}
		
		if (null != oneTimeKeysBuffer)
		{
			JsonObject oneTimeKeysJson = (JsonObject) new Parser().parse(new ByteArrayInputStream(oneTimeKeysBuffer), UTF_8);
			if (oneTimeKeysJson == null)
				throw new OlmException(EXCEPTION_CODE_ACCOUNT_ONE_TIME_KEYS, "failed to parse json");
			return oneTimeKeysJson;
		}
		else
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_ONE_TIME_KEYS, "oneTimeKeysJni()=null");
	}
	
	/**
	 * Get the public parts of the unpublished "one time keys" for the account.<br>
	 * The returned data is a JSON-formatted object with the single property
	 * <tt>curve25519</tt>, which is itself an object mapping key id to
	 * base64-encoded Curve25519 key.<br>
	 *
	 * @return byte array containing the one time keys or throw an exception if it fails
	 */
	private native byte[] oneTimeKeysJni();
	
	/**
	 * Remove the "one time keys" that the session used from the account.
	 *
	 * @param aSession session instance
	 * @throws OlmException the failure reason
	 */
	public void removeOneTimeKeys(@Nonnull OlmSession aSession)
			throws OlmException
	{
		try
		{
			removeOneTimeKeysJni(aSession.getOlmSessionId());
		}
		catch (Exception e)
		{
			throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_REMOVE_ONE_TIME_KEYS, e.getMessage());
		}
	}
	
	/**
	 * Remove the "one time keys" that the session used from the account.
	 * An exception is thrown if the operation fails.
	 *
	 * @param aNativeOlmSessionId native session instance identifier
	 */
	private native void removeOneTimeKeysJni(long aNativeOlmSessionId);
	
	/**
	 * Marks the current set of "one time keys" as being published.
	 *
	 * @throws OlmException the failure reason
	 */
	public void markOneTimeKeysAsPublished()
			throws OlmException
	{
		try
		{
			markOneTimeKeysAsPublishedJni();
		}
		catch (Exception e)
		{
			throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_MARK_ONE_KEYS_AS_PUBLISHED, e.getMessage());
		}
	}
	
	/**
	 * Marks the current set of "one time keys" as being published.
	 * An exception is thrown if the operation fails.
	 */
	private native void markOneTimeKeysAsPublishedJni();
	
	/**
	 * Sign a message with the ed25519 fingerprint key for this account.<br>
	 * The signed message is returned by the method.
	 *
	 * @param aMessage message to sign
	 * @return the signed message
	 * @throws OlmException the failure reason
	 */
	@Nonnull
	public String signMessage(@Nonnull String aMessage)
			throws OlmException
	{
		byte[] signedMessage = null;
		
		try
		{
			byte[] utf8String = aMessage.getBytes(UTF_8);
			
			signedMessage = signMessageJni(utf8String);
		}
		catch (Exception e)
		{
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_SIGN_MESSAGE, e.getMessage());
		}
		
		if (signedMessage == null)
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_SIGN_MESSAGE, "signMessageJni()=null");
		
		return new String(signedMessage, UTF_8);
	}
	
	/**
	 * Sign a message with the ed25519 fingerprint key for this account.<br>
	 * The signed message is returned by the method.
	 *
	 * @param aMessage message to sign
	 * @return the signed message
	 */
	private native byte[] signMessageJni(byte[] aMessage);
	
	//==============================================================================================================
	// Serialization management
	//==============================================================================================================
	
	/**
	 * Kick off the serialization mechanism.
	 *
	 * @param aOutStream output stream for serializing
	 * @throws IOException exception
	 */
	private void writeObject(@Nonnull ObjectOutputStream aOutStream)
			throws IOException
	{
		serialize(aOutStream);
	}
	
	/**
	 * Kick off the deserialization mechanism.
	 *
	 * @param aInStream input stream
	 * @throws Exception exception
	 */
	private void readObject(@Nonnull ObjectInputStream aInStream)
			throws Exception
	{
		deserialize(aInStream);
	}
	
	/**
	 * Return an account as a bytes buffer.<br>
	 * The account is serialized and encrypted with aKey.
	 * In case of failure, an error human readable
	 * description is provide in aErrorMsg.
	 *
	 * @param aKey      encryption key
	 * @param aErrorMsg error message description
	 * @return the account as bytes buffer
	 */
	@Override
	@Nullable
	protected byte[] serialize(@Nonnull byte[] aKey, @Nonnull StringBuffer aErrorMsg)
	{
		byte[] pickleRetValue = null;
		
		aErrorMsg.setLength(0);
		try
		{
			pickleRetValue = serializeJni(aKey);
		}
		catch (Exception e)
		{
			LOGGER.error("## serialize() failed " + e.getMessage());
			aErrorMsg.append(e.getMessage());
		}
		
		return pickleRetValue;
	}
	
	/**
	 * Serialize and encrypt account instance.<br>
	 *
	 * @param aKeyBuffer key used to encrypt the serialized account data
	 * @return the serialised account as bytes buffer.
	 **/
	private native byte[] serializeJni(byte[] aKeyBuffer);
	
	/**
	 * Loads an account from a pickled bytes buffer.<br>
	 * See {@link #serialize(byte[], StringBuffer)}
	 *
	 * @param aSerializedData bytes buffer
	 * @param aKey            key used to encrypted
	 * @throws Exception the exception
	 */
	@Override
	protected void deserialize(@Nonnull byte[] aSerializedData, @Nonnull byte[] aKey)
			throws Exception
	{
		String errorMsg = null;
		
		try
		{
			mNativeId = deserializeJni(aSerializedData, aKey);
		}
		catch (Exception e)
		{
			LOGGER.error("## deserialize() failed " + e.getMessage());
			errorMsg = e.getMessage();
		}
		
		if (errorMsg != null)
		{
			releaseAccount();
			throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_DESERIALIZATION, errorMsg);
		}
	}
	
	/**
	 * Allocate a new account and initialize it with the serialisation data.<br>
	 *
	 * @param aSerializedDataBuffer the account serialisation buffer
	 * @param aKeyBuffer            the key used to encrypt the serialized account data
	 * @return the deserialized account
	 **/
	private native long deserializeJni(byte[] aSerializedDataBuffer, byte[] aKeyBuffer);
}
