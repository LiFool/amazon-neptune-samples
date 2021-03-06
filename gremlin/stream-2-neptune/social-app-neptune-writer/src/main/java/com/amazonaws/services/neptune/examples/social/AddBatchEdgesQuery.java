/*
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy of this
software and associated documentation files (the "Software"), to deal in the Software
without restriction, including without limitation the rights to use, copy, modify,
merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.amazonaws.services.neptune.examples.social;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.neptune.examples.utils.ActivityTimer;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.T;

import java.util.UUID;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.V;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.unfold;

class AddBatchEdgesQuery {

    private GraphTraversal traversal;
    private final LambdaLogger logger;
    private final boolean conditionalCreate;

    AddBatchEdgesQuery(NeptuneClient neptuneClient, LambdaLogger logger, boolean conditionalCreate) {
        this.traversal = neptuneClient.newTraversal();
        this.logger = logger;
        this.conditionalCreate = conditionalCreate;
    }

    void addEdge(String fromVertexId, String toVertexId, String creationDate, long insertDateTime) {

        String edgeId = conditionalCreate ?
                String.format("%s-%s-%s", fromVertexId, creationDate, toVertexId) :
                UUID.randomUUID().toString();

        if (conditionalCreate) {
            traversal = traversal.V(fromVertexId).outE("follows").hasId(edgeId).fold().coalesce(
                    unfold(),
                    V(fromVertexId).addE("follows").to(V(toVertexId)).
                            property(T.id, edgeId).
                            property("creationDate", creationDate).
                            property("insertDateTime", insertDateTime));
        } else {
            traversal = traversal.V(fromVertexId).addE("follows").to(V(toVertexId)).
                    property(T.id, edgeId).
                    property("creationDate", creationDate).
                    property("insertDateTime", insertDateTime);
        }
    }

    void provokeError() {
        logger.log("Forcing a ConstraintViolationException (and rollback)");

        traversal = traversal.
                addV("error").property(T.id, "error").
                addV("error").property(T.id, "error");
    }

    long execute(int batchId) {
        ActivityTimer timer = new ActivityTimer(logger, "Execute query [" + batchId + "]");
        traversal.forEachRemaining(e -> {
        });
        return timer.stop();
    }
}
