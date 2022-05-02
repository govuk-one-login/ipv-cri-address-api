package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DescribeKeyRequest;
import com.amazonaws.services.kms.model.DescribeKeyResult;
import com.amazonaws.services.kms.model.GetPublicKeyRequest;
import com.amazonaws.services.kms.model.GetPublicKeyResult;
import com.amazonaws.services.kms.model.KeyListEntry;
import com.amazonaws.services.kms.model.KeyMetadata;
import com.amazonaws.services.kms.model.ListResourceTagsRequest;
import com.amazonaws.services.kms.model.ListResourceTagsResult;
import com.amazonaws.services.kms.model.Tag;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.List;

public class KMSService {

    private final AWSKMS kmsClient;

    public KMSService() {
        this.kmsClient = AWSKMSClientBuilder.defaultClient();
    }

    public KMSService(AWSKMS kmsClient) {
        this.kmsClient = kmsClient;
    }

    public List<KeyListEntry> getKeys() {
        return kmsClient.listKeys().getKeys();
    }

    public List<Tag> getTags(String keyId) {
        ListResourceTagsRequest listResourceTagsRequest = new ListResourceTagsRequest();
        listResourceTagsRequest.setKeyId(keyId);
        ListResourceTagsResult listResourceTagsResult =
                kmsClient.listResourceTags(listResourceTagsRequest);
        return listResourceTagsResult.getTags();
    }

    public KeyMetadata getMetadata(String keyId) {
        DescribeKeyRequest describeKeyRequest = new DescribeKeyRequest();
        describeKeyRequest.setKeyId(keyId);
        DescribeKeyResult describeKeyResult = kmsClient.describeKey(describeKeyRequest);
        return describeKeyResult.getKeyMetadata();
    }

    public JWK getJWK(String keyId) {
        GetPublicKeyRequest publicKeyRequest = new GetPublicKeyRequest();
        publicKeyRequest.setKeyId(keyId);
        GetPublicKeyResult publicKeyResult = kmsClient.getPublicKey(publicKeyRequest);
        EncodedKeySpec publicKeySpec =
                new X509EncodedKeySpec(publicKeyResult.getPublicKey().array());

        try {

            Algorithm algorithm = Algorithm.parse(publicKeySpec.getAlgorithm());
            String keyUsage = publicKeyResult.getKeyUsage();
            if (publicKeyResult.getKeySpec().startsWith("RSA")) {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");

                RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
                return new RSAKey.Builder(publicKey)
                        .keyID(keyId)
                        .algorithm(algorithm)
                        .keyUse(KeyUse.parse(keyUsage))
                        .build();
            } else {
                KeyFactory keyFactory = KeyFactory.getInstance("EC");

                ECPublicKey publicKey = (ECPublicKey) keyFactory.generatePublic(publicKeySpec);
                return new ECKey.Builder(Curve.P_256, publicKey)
                        .keyID(keyId)
                        .algorithm(algorithm)
                        .keyUse(KeyUse.parse(keyUsage))
                        .build();
            }
        } catch (ParseException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        }
    }
}
