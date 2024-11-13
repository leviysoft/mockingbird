var bulk = db.mockingbirdGrpcStubs.initializeOrderedBulkOp();

db.mockingbirdGrpcStubs
    .find( { "methodName": {$exists: true} } )
    .sort( { "scope": 1 } )
    .forEach(function (doc) {
        var existedMethodDescription = db.mockingbirdGrpcMethodDescriptions.findOne({"methodName": doc.methodName});

        let methodDescriptionId;
        if (existedMethodDescription)
            methodDescriptionId = existedMethodDescription._id;
        else {
            var methodDescriptionDoc = {
                "_id": UUID().toString().split('"')[1],
                "description": "",
                "created": new Date(),
                "service": doc.service,
                "methodName": doc.methodName,
                "connectionType": "UNARY",
                "requestClass": doc.requestClass,
                "requestSchema": doc.requestSchema,
                "responseClass": doc.responseClass,
                "responseSchema": doc.responseSchema
            }

            db.mockingbirdGrpcMethodDescriptions.insertOne(
                methodDescriptionDoc
            );
            methodDescriptionId = methodDescriptionDoc._id;
        }

        if (doc.proxyUrl)
            db.mockingbirdGrpcMethodDescriptions.updateOne(
                { "_id": methodDescriptionId, "proxyUrl": {$exists: false} },
                { $set: {"proxyUrl": doc.proxyUrl} }
            );

        var setPatch = {
            "methodDescriptionId": methodDescriptionId
        };
        var unsetPatch = {
            "methodName": "",
            "service": "",
            "requestSchema": "",
            "requestClass": "",
            "responseSchema": "",
            "responseClass": ""
        };

        bulk
            .find( { "_id": doc._id } )
            .updateOne(
            {
                $set: setPatch,
                $unset: unsetPatch
            }
        );
    });
if (bulk.length > 0) {
    bulk.execute();
}
