package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.kms.model.KeyMetadata;
import com.amazonaws.services.kms.model.Tag;
import com.nimbusds.jose.jwk.JWK;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public class JWKSetService {

    public static final String ENV_VAR_NAME_CLOUDFORMATION_STACK = "AWS_STACK_NAME";

    private final KMSService kmsService;

    private final EnvironmentService environmentService;

    public JWKSetService() {
        this.environmentService = new EnvironmentService();
        this.kmsService = new KMSService();
    }

    public JWKSetService(KMSService kmsService, EnvironmentService environmentService) {
        this.kmsService = kmsService;
        this.environmentService = environmentService;
    }

    public List<JWK> getJWKs() {
        Set<Tag> tagsToMatch = Set.of(getStackNameTagForPublish(), getJWKSetTagForPublish());
        return getKMSKeyIds().stream()
                .filter(keyId -> matchOnTags(keyId, tagsToMatch))
                .filter(this::findEnabled)
                .map(this::mapToJWK)
                .collect(toList());
    }

    private List<String> getKMSKeyIds() {
        return kmsService.getKeyIds();
    }

    private JWK mapToJWK(String keyId) {
        return kmsService.getJWK(keyId);
    }

    protected boolean matchOnTags(String keyId, Set<Tag> tagsForPublish) {
        Set<Tag> mutableCopy = new HashSet<>(tagsForPublish);
        Set<Tag> tags = kmsService.getTags(keyId);
        return !mutableCopy.retainAll(tags);
    }

    private Tag getStackNameTagForPublish() {
        String stackNameForThisFunction =
                environmentService.getEnvironmentVariableOrThrow(ENV_VAR_NAME_CLOUDFORMATION_STACK);
        return new Tag().withTagKey("awsStackName").withTagValue(stackNameForThisFunction);
    }

    private Tag getJWKSetTagForPublish() {
        return new Tag().withTagKey("jwkset").withTagValue("true");
    }

    private boolean findEnabled(String keyId) {
        KeyMetadata keyMetadata = kmsService.getMetadata(keyId);
        return "Enabled".equals(keyMetadata.getKeyState());
    }
}
