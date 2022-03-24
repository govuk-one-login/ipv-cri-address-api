package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.helpers.EventProbe;

@ExtendWith(MockitoExtension.class)
public class AddressHandlerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock private Context context;
    private EventProbe eventProbe = new EventProbe();
}
