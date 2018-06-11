package jce.des;


import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class DESSecretKey {

    private SecretKey key;

    public DESSecretKey() {
        try
        {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("DES");
//            keyGenerator.init(64);
            this.key = keyGenerator.generateKey();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }

    public DESSecretKey(SecretKey key)
    {
        this.key = key;
    }

    public SecretKey getKey()
    {
        return this.key;
    }

    public static DESSecretKey generateKeyFromBase64(String base64Key)
    {
        byte[] tmp = Base64.decodeBase64(base64Key);
        return new DESSecretKey(new SecretKeySpec(tmp, "DES"));
    }

    @Override
    public String toString()
    {
        return new String(Base64.encodeBase64(key.getEncoded()));
    }
}
