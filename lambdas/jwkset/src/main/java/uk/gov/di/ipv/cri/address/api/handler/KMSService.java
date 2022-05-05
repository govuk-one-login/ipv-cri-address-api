package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DescribeKeyRequest;
import com.amazonaws.services.kms.model.DescribeKeyResult;
import com.amazonaws.services.kms.model.GetPublicKeyRequest;
import com.amazonaws.services.kms.model.GetPublicKeyResult;
import com.amazonaws.services.kms.model.KeyListEntry;
import com.amazonaws.services.kms.model.KeyMetadata;
import com.amazonaws.services.kms.model.ListKeysRequest;
import com.amazonaws.services.kms.model.ListKeysResult;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class KMSService {

    private final AWSKMS kmsClient;

    public KMSService() {
        this.kmsClient = AWSKMSClientBuilder.defaultClient();
    }

    public KMSService(AWSKMS kmsClient) {
        this.kmsClient = kmsClient;
    }

    public List<String> getKeyIds() {
        ListKeysRequest listKeysRequest = new ListKeysRequest();
        ListKeysResult result = kmsClient.listKeys(listKeysRequest);
        List<KeyListEntry> keyListEntries = new ArrayList<>(result.getKeys());
        while (result.isTruncated()) {
            listKeysRequest = new ListKeysRequest().withMarker(result.getNextMarker());
            result = kmsClient.listKeys(listKeysRequest);
            keyListEntries.addAll(result.getKeys());
        }
        return keyListEntries.stream().map(KeyListEntry::getKeyId).collect(Collectors.toList());
    }

    public Set<Tag> getTags(String keyId) {
        ListResourceTagsResult listResourceTagsResult =
                kmsClient.listResourceTags(new ListResourceTagsRequest().withKeyId(keyId));
        return new HashSet<>(listResourceTagsResult.getTags());
    }

    public KeyMetadata getMetadata(String keyId) {
        DescribeKeyResult describeKeyResult =
                kmsClient.describeKey(new DescribeKeyRequest().withKeyId(keyId));
        return describeKeyResult.getKeyMetadata();
    }

    public JWK getJWK(String keyId) {
        GetPublicKeyResult publicKeyResult =
                kmsClient.getPublicKey(new GetPublicKeyRequest().withKeyId(keyId));
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
