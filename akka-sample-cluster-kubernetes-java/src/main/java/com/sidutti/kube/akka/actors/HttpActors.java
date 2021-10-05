package com.sidutti.kube.akka.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Adapter;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.ClusterEvent;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Subscribe;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.scaladsl.AkkaManagement;
import akka.stream.Materializer;

import static akka.http.javadsl.server.Directives.complete;

public class HttpActors {
    static class MemberEventLogger {
        public static Behavior<ClusterEvent.MemberEvent> create() {
            return Behaviors.setup(context -> {
                Cluster cluster = Cluster.get(context.getSystem());

                context.getLog().info("Started [{}], cluster.selfAddress = {})",
                        context.getSystem(),
                        cluster.selfMember().address());

                cluster.subscriptions().tell(new Subscribe<>(context.getSelf(), ClusterEvent.MemberEvent.class));

                return Behaviors.receiveMessage(event -> {
                    context.getLog().info("MemberEvent: {}", event);
                    return Behaviors.same();
                });
            });
        }
    }

    static class Guardian {
        public static Behavior<Void> create() {
            return Behaviors.setup(context -> {
                final akka.actor.ActorSystem classicSystem = Adapter.toClassic(context.getSystem());
                Materializer mat = Materializer.matFromSystem(classicSystem);

                Http.get(classicSystem).bindAndHandle(complete("Hello world")
                                .flow(classicSystem, mat), ConnectHttp.toHost("0.0.0.0", 8080), mat)
                        .whenComplete((binding, failure) -> {
                            if (failure == null) {
                                classicSystem.log().info("HTTP server now listening at port 8080");
                            } else {
                                classicSystem.log().error(failure, "Failed to bind HTTP server, terminating.");
                                classicSystem.terminate();
                            }
                        });

                context.spawn(MemberEventLogger.create(), "listener");

                AkkaManagement.get(classicSystem).start();
                ClusterBootstrap.get(classicSystem).start();

                return Behaviors.empty();
            });
        }
    }
}
