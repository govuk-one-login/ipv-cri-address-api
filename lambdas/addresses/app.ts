import { APIGatewayProxyEvent, APIGatewayProxyResult } from 'aws-lambda';
import { addressLookupService } from './service/addressLookup-service';

export const handler = async (event: APIGatewayProxyEvent): Promise<APIGatewayProxyResult> => {
    
    let response: APIGatewayProxyResult;
    
    try {
        let sessionId = event.headers['session_id'] + '';
        
        const result = await addressLookupService.getAddressBySessionId(sessionId);

        response = {
                statusCode: 200,
                body: JSON.stringify({
                    result
                }),
            };
       
    } catch (err) {
        console.log(err);
        response = {
            statusCode: 400,
            body: 'No data found.',
        };
        await handleError(err);
    }
    return response;
   
};

const handleError = async (err: any) => {
    const error_object = {
        reason: 'An error has occurred.',
        traceback: err.stack,
    };
    console.error(`Rejecting AddressLookup request ${JSON.stringify(error_object)}`);
};
