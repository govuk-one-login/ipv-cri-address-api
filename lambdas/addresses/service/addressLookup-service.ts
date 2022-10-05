import { GetCommand } from "@aws-sdk/lib-dynamodb";
import { dynamoDbDocumentClient } from './dynamoDbClient';
import config from '../config';

export class AddressLookupService {

    constructor(private tableName: string){}
    
        public async getAddressBySessionId(
            sessionId: string
          ): Promise<any> {

            try {
                console.log('tableName ==>'+this.tableName + ' sessionId: '+sessionId);

                let params = {
                  TableName: this.tableName,
                  Key: {
                    sessionId: sessionId
                  },
                };
              const result = await dynamoDbDocumentClient.send(new GetCommand(params));
              return result.Item;
            } catch (e) {
              console.error(
                "Error retrieving address item from dynamodb",
                e as Error
              );
              throw e;
            }
          } 

}

export const addressLookupService = new AddressLookupService(
    config.addressLookupStorageTableName
  );


