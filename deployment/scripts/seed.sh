#!/bin/bash

## Seed asset/policy/contract-def data to both "provider-qna" and "provider-manufacturing"
for url in 'http://127.0.0.1:4081' 'http://127.0.0.1:5081'
do
  newman run \
    --folder "Seed" \
    --env-var "HOST=$url" \
    ../postman/MVD.postman_collection.json
done

## Seed linked assets to Catalog Server "Bob"
newman run \
  --folder "Seed Catalog Server" \
  --env-var "HOST=http://127.0.0.1:9081" \
  --env-var "PROVIDER_QNA_DSP_URL=http://10.0.40.172:4082" \
  --env-var "PROVIDER_MF_DSP_URL=http://10.0.40.172:5082" \
  ../postman/MVD.postman_collection.json

## Seed management DATA to identityhubs
API_KEY="c3VwZXItdXNlcg==.c3VwZXItc2VjcmV0LWtleQo="

# add provider participant
echo
echo
echo "Create provider participant"
PROVIDER_CATALOG_SERVER_URL="http://10.0.40.172:9082"
PROVIDER_IDENTITYHUB_URL="http://10.0.40.172:7082"
DATA_PROVIDER=$(jq -n --arg url "$PROVIDER_CATALOG_SERVER_URL" --arg ihurl "$PROVIDER_IDENTITYHUB_URL" '{
           "roles":[],
           "serviceEndpoints":[
             {
                "type": "CredentialService",
                "serviceEndpoint": "\($ihurl)/api/presentation/v1/participants/ZGlkOndlYjoxOTIuMTY4LjEuMTAyJTNBNzA4Mzpwcm92aWRlcg==",
                "id": "provider-credentialservice-1"
             },
             {
                "type": "ProtocolEndpoint",
                "serviceEndpoint": "\($url)/api/dsp",
                "id": "provider-dsp"
             }
           ],
           "active": true,
           "participantId": "did:web:10.0.40.172%3A7083:provider",
           "did": "did:web:10.0.40.172%3A7083:provider",
           "key":{
               "keyId": "did:web:10.0.40.172%3A7083:provider#key-1",
               "privateKeyAlias": "did:web:10.0.40.172%3A7083:provider#key-1",
               "keyGeneratorParams":{
                  "algorithm": "EC"
               }
           }
       }')

curl --location 'http://localhost:7081/api/identity/v1alpha/participants/' \
--header 'Content-Type: application/json' \
--header "x-api-key: $API_KEY" \
--data "$DATA_PROVIDER"
