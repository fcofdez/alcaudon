package firmament;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.6.0-SNAPSHOT)",
    comments = "Source: firmament_scheduler.proto")
public final class FirmamentSchedulerGrpc {

  private FirmamentSchedulerGrpc() {}

  public static final String SERVICE_NAME = "firmament.FirmamentScheduler";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<firmament.FirmamentSchedulerOuterClass.ScheduleRequest,
      firmament.FirmamentSchedulerOuterClass.SchedulingDeltas> METHOD_SCHEDULE =
      io.grpc.MethodDescriptor.<firmament.FirmamentSchedulerOuterClass.ScheduleRequest, firmament.FirmamentSchedulerOuterClass.SchedulingDeltas>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(generateFullMethodName(
              "firmament.FirmamentScheduler", "Schedule"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.ScheduleRequest.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.SchedulingDeltas.getDefaultInstance()))
          .build();
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<firmament.FirmamentSchedulerOuterClass.TaskUID,
      firmament.FirmamentSchedulerOuterClass.TaskCompletedResponse> METHOD_TASK_COMPLETED =
      io.grpc.MethodDescriptor.<firmament.FirmamentSchedulerOuterClass.TaskUID, firmament.FirmamentSchedulerOuterClass.TaskCompletedResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(generateFullMethodName(
              "firmament.FirmamentScheduler", "TaskCompleted"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.TaskUID.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.TaskCompletedResponse.getDefaultInstance()))
          .build();
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<firmament.FirmamentSchedulerOuterClass.TaskUID,
      firmament.FirmamentSchedulerOuterClass.TaskFailedResponse> METHOD_TASK_FAILED =
      io.grpc.MethodDescriptor.<firmament.FirmamentSchedulerOuterClass.TaskUID, firmament.FirmamentSchedulerOuterClass.TaskFailedResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(generateFullMethodName(
              "firmament.FirmamentScheduler", "TaskFailed"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.TaskUID.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.TaskFailedResponse.getDefaultInstance()))
          .build();
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<firmament.FirmamentSchedulerOuterClass.TaskUID,
      firmament.FirmamentSchedulerOuterClass.TaskRemovedResponse> METHOD_TASK_REMOVED =
      io.grpc.MethodDescriptor.<firmament.FirmamentSchedulerOuterClass.TaskUID, firmament.FirmamentSchedulerOuterClass.TaskRemovedResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(generateFullMethodName(
              "firmament.FirmamentScheduler", "TaskRemoved"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.TaskUID.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.TaskRemovedResponse.getDefaultInstance()))
          .build();
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<firmament.FirmamentSchedulerOuterClass.TaskDescription,
      firmament.FirmamentSchedulerOuterClass.TaskSubmittedResponse> METHOD_TASK_SUBMITTED =
      io.grpc.MethodDescriptor.<firmament.FirmamentSchedulerOuterClass.TaskDescription, firmament.FirmamentSchedulerOuterClass.TaskSubmittedResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(generateFullMethodName(
              "firmament.FirmamentScheduler", "TaskSubmitted"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.TaskDescription.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.TaskSubmittedResponse.getDefaultInstance()))
          .build();
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<firmament.FirmamentSchedulerOuterClass.TaskDescription,
      firmament.FirmamentSchedulerOuterClass.TaskUpdatedResponse> METHOD_TASK_UPDATED =
      io.grpc.MethodDescriptor.<firmament.FirmamentSchedulerOuterClass.TaskDescription, firmament.FirmamentSchedulerOuterClass.TaskUpdatedResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(generateFullMethodName(
              "firmament.FirmamentScheduler", "TaskUpdated"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.TaskDescription.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.TaskUpdatedResponse.getDefaultInstance()))
          .build();
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor,
      firmament.FirmamentSchedulerOuterClass.NodeAddedResponse> METHOD_NODE_ADDED =
      io.grpc.MethodDescriptor.<firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor, firmament.FirmamentSchedulerOuterClass.NodeAddedResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(generateFullMethodName(
              "firmament.FirmamentScheduler", "NodeAdded"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.NodeAddedResponse.getDefaultInstance()))
          .build();
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<firmament.FirmamentSchedulerOuterClass.ResourceUID,
      firmament.FirmamentSchedulerOuterClass.NodeFailedResponse> METHOD_NODE_FAILED =
      io.grpc.MethodDescriptor.<firmament.FirmamentSchedulerOuterClass.ResourceUID, firmament.FirmamentSchedulerOuterClass.NodeFailedResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(generateFullMethodName(
              "firmament.FirmamentScheduler", "NodeFailed"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.ResourceUID.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.NodeFailedResponse.getDefaultInstance()))
          .build();
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<firmament.FirmamentSchedulerOuterClass.ResourceUID,
      firmament.FirmamentSchedulerOuterClass.NodeRemovedResponse> METHOD_NODE_REMOVED =
      io.grpc.MethodDescriptor.<firmament.FirmamentSchedulerOuterClass.ResourceUID, firmament.FirmamentSchedulerOuterClass.NodeRemovedResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(generateFullMethodName(
              "firmament.FirmamentScheduler", "NodeRemoved"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.ResourceUID.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.NodeRemovedResponse.getDefaultInstance()))
          .build();
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor,
      firmament.FirmamentSchedulerOuterClass.NodeUpdatedResponse> METHOD_NODE_UPDATED =
      io.grpc.MethodDescriptor.<firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor, firmament.FirmamentSchedulerOuterClass.NodeUpdatedResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(generateFullMethodName(
              "firmament.FirmamentScheduler", "NodeUpdated"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.NodeUpdatedResponse.getDefaultInstance()))
          .build();
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<firmament.TaskStatsOuterClass.TaskStats,
      firmament.FirmamentSchedulerOuterClass.TaskStatsResponse> METHOD_ADD_TASK_STATS =
      io.grpc.MethodDescriptor.<firmament.TaskStatsOuterClass.TaskStats, firmament.FirmamentSchedulerOuterClass.TaskStatsResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(generateFullMethodName(
              "firmament.FirmamentScheduler", "AddTaskStats"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.TaskStatsOuterClass.TaskStats.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.TaskStatsResponse.getDefaultInstance()))
          .build();
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<firmament.ResourceStatsOuterClass.ResourceStats,
      firmament.FirmamentSchedulerOuterClass.ResourceStatsResponse> METHOD_ADD_NODE_STATS =
      io.grpc.MethodDescriptor.<firmament.ResourceStatsOuterClass.ResourceStats, firmament.FirmamentSchedulerOuterClass.ResourceStatsResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(generateFullMethodName(
              "firmament.FirmamentScheduler", "AddNodeStats"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.ResourceStatsOuterClass.ResourceStats.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              firmament.FirmamentSchedulerOuterClass.ResourceStatsResponse.getDefaultInstance()))
          .build();

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static FirmamentSchedulerStub newStub(io.grpc.Channel channel) {
    return new FirmamentSchedulerStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static FirmamentSchedulerBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new FirmamentSchedulerBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static FirmamentSchedulerFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new FirmamentSchedulerFutureStub(channel);
  }

  /**
   */
  public static abstract class FirmamentSchedulerImplBase implements io.grpc.BindableService {

    /**
     */
    public void schedule(firmament.FirmamentSchedulerOuterClass.ScheduleRequest request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.SchedulingDeltas> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_SCHEDULE, responseObserver);
    }

    /**
     */
    public void taskCompleted(firmament.FirmamentSchedulerOuterClass.TaskUID request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskCompletedResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_TASK_COMPLETED, responseObserver);
    }

    /**
     */
    public void taskFailed(firmament.FirmamentSchedulerOuterClass.TaskUID request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskFailedResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_TASK_FAILED, responseObserver);
    }

    /**
     */
    public void taskRemoved(firmament.FirmamentSchedulerOuterClass.TaskUID request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskRemovedResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_TASK_REMOVED, responseObserver);
    }

    /**
     */
    public void taskSubmitted(firmament.FirmamentSchedulerOuterClass.TaskDescription request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskSubmittedResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_TASK_SUBMITTED, responseObserver);
    }

    /**
     */
    public void taskUpdated(firmament.FirmamentSchedulerOuterClass.TaskDescription request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskUpdatedResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_TASK_UPDATED, responseObserver);
    }

    /**
     */
    public void nodeAdded(firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.NodeAddedResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_NODE_ADDED, responseObserver);
    }

    /**
     */
    public void nodeFailed(firmament.FirmamentSchedulerOuterClass.ResourceUID request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.NodeFailedResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_NODE_FAILED, responseObserver);
    }

    /**
     */
    public void nodeRemoved(firmament.FirmamentSchedulerOuterClass.ResourceUID request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.NodeRemovedResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_NODE_REMOVED, responseObserver);
    }

    /**
     */
    public void nodeUpdated(firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.NodeUpdatedResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_NODE_UPDATED, responseObserver);
    }

    /**
     */
    public void addTaskStats(firmament.TaskStatsOuterClass.TaskStats request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskStatsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_ADD_TASK_STATS, responseObserver);
    }

    /**
     */
    public void addNodeStats(firmament.ResourceStatsOuterClass.ResourceStats request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.ResourceStatsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_ADD_NODE_STATS, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_SCHEDULE,
            asyncUnaryCall(
              new MethodHandlers<
                firmament.FirmamentSchedulerOuterClass.ScheduleRequest,
                firmament.FirmamentSchedulerOuterClass.SchedulingDeltas>(
                  this, METHODID_SCHEDULE)))
          .addMethod(
            METHOD_TASK_COMPLETED,
            asyncUnaryCall(
              new MethodHandlers<
                firmament.FirmamentSchedulerOuterClass.TaskUID,
                firmament.FirmamentSchedulerOuterClass.TaskCompletedResponse>(
                  this, METHODID_TASK_COMPLETED)))
          .addMethod(
            METHOD_TASK_FAILED,
            asyncUnaryCall(
              new MethodHandlers<
                firmament.FirmamentSchedulerOuterClass.TaskUID,
                firmament.FirmamentSchedulerOuterClass.TaskFailedResponse>(
                  this, METHODID_TASK_FAILED)))
          .addMethod(
            METHOD_TASK_REMOVED,
            asyncUnaryCall(
              new MethodHandlers<
                firmament.FirmamentSchedulerOuterClass.TaskUID,
                firmament.FirmamentSchedulerOuterClass.TaskRemovedResponse>(
                  this, METHODID_TASK_REMOVED)))
          .addMethod(
            METHOD_TASK_SUBMITTED,
            asyncUnaryCall(
              new MethodHandlers<
                firmament.FirmamentSchedulerOuterClass.TaskDescription,
                firmament.FirmamentSchedulerOuterClass.TaskSubmittedResponse>(
                  this, METHODID_TASK_SUBMITTED)))
          .addMethod(
            METHOD_TASK_UPDATED,
            asyncUnaryCall(
              new MethodHandlers<
                firmament.FirmamentSchedulerOuterClass.TaskDescription,
                firmament.FirmamentSchedulerOuterClass.TaskUpdatedResponse>(
                  this, METHODID_TASK_UPDATED)))
          .addMethod(
            METHOD_NODE_ADDED,
            asyncUnaryCall(
              new MethodHandlers<
                firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor,
                firmament.FirmamentSchedulerOuterClass.NodeAddedResponse>(
                  this, METHODID_NODE_ADDED)))
          .addMethod(
            METHOD_NODE_FAILED,
            asyncUnaryCall(
              new MethodHandlers<
                firmament.FirmamentSchedulerOuterClass.ResourceUID,
                firmament.FirmamentSchedulerOuterClass.NodeFailedResponse>(
                  this, METHODID_NODE_FAILED)))
          .addMethod(
            METHOD_NODE_REMOVED,
            asyncUnaryCall(
              new MethodHandlers<
                firmament.FirmamentSchedulerOuterClass.ResourceUID,
                firmament.FirmamentSchedulerOuterClass.NodeRemovedResponse>(
                  this, METHODID_NODE_REMOVED)))
          .addMethod(
            METHOD_NODE_UPDATED,
            asyncUnaryCall(
              new MethodHandlers<
                firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor,
                firmament.FirmamentSchedulerOuterClass.NodeUpdatedResponse>(
                  this, METHODID_NODE_UPDATED)))
          .addMethod(
            METHOD_ADD_TASK_STATS,
            asyncUnaryCall(
              new MethodHandlers<
                firmament.TaskStatsOuterClass.TaskStats,
                firmament.FirmamentSchedulerOuterClass.TaskStatsResponse>(
                  this, METHODID_ADD_TASK_STATS)))
          .addMethod(
            METHOD_ADD_NODE_STATS,
            asyncUnaryCall(
              new MethodHandlers<
                firmament.ResourceStatsOuterClass.ResourceStats,
                firmament.FirmamentSchedulerOuterClass.ResourceStatsResponse>(
                  this, METHODID_ADD_NODE_STATS)))
          .build();
    }
  }

  /**
   */
  public static final class FirmamentSchedulerStub extends io.grpc.stub.AbstractStub<FirmamentSchedulerStub> {
    private FirmamentSchedulerStub(io.grpc.Channel channel) {
      super(channel);
    }

    private FirmamentSchedulerStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FirmamentSchedulerStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new FirmamentSchedulerStub(channel, callOptions);
    }

    /**
     */
    public void schedule(firmament.FirmamentSchedulerOuterClass.ScheduleRequest request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.SchedulingDeltas> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_SCHEDULE, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void taskCompleted(firmament.FirmamentSchedulerOuterClass.TaskUID request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskCompletedResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_TASK_COMPLETED, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void taskFailed(firmament.FirmamentSchedulerOuterClass.TaskUID request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskFailedResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_TASK_FAILED, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void taskRemoved(firmament.FirmamentSchedulerOuterClass.TaskUID request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskRemovedResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_TASK_REMOVED, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void taskSubmitted(firmament.FirmamentSchedulerOuterClass.TaskDescription request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskSubmittedResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_TASK_SUBMITTED, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void taskUpdated(firmament.FirmamentSchedulerOuterClass.TaskDescription request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskUpdatedResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_TASK_UPDATED, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void nodeAdded(firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.NodeAddedResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_NODE_ADDED, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void nodeFailed(firmament.FirmamentSchedulerOuterClass.ResourceUID request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.NodeFailedResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_NODE_FAILED, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void nodeRemoved(firmament.FirmamentSchedulerOuterClass.ResourceUID request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.NodeRemovedResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_NODE_REMOVED, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void nodeUpdated(firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.NodeUpdatedResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_NODE_UPDATED, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void addTaskStats(firmament.TaskStatsOuterClass.TaskStats request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskStatsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_ADD_TASK_STATS, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void addNodeStats(firmament.ResourceStatsOuterClass.ResourceStats request,
        io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.ResourceStatsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_ADD_NODE_STATS, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class FirmamentSchedulerBlockingStub extends io.grpc.stub.AbstractStub<FirmamentSchedulerBlockingStub> {
    private FirmamentSchedulerBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private FirmamentSchedulerBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FirmamentSchedulerBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new FirmamentSchedulerBlockingStub(channel, callOptions);
    }

    /**
     */
    public firmament.FirmamentSchedulerOuterClass.SchedulingDeltas schedule(firmament.FirmamentSchedulerOuterClass.ScheduleRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_SCHEDULE, getCallOptions(), request);
    }

    /**
     */
    public firmament.FirmamentSchedulerOuterClass.TaskCompletedResponse taskCompleted(firmament.FirmamentSchedulerOuterClass.TaskUID request) {
      return blockingUnaryCall(
          getChannel(), METHOD_TASK_COMPLETED, getCallOptions(), request);
    }

    /**
     */
    public firmament.FirmamentSchedulerOuterClass.TaskFailedResponse taskFailed(firmament.FirmamentSchedulerOuterClass.TaskUID request) {
      return blockingUnaryCall(
          getChannel(), METHOD_TASK_FAILED, getCallOptions(), request);
    }

    /**
     */
    public firmament.FirmamentSchedulerOuterClass.TaskRemovedResponse taskRemoved(firmament.FirmamentSchedulerOuterClass.TaskUID request) {
      return blockingUnaryCall(
          getChannel(), METHOD_TASK_REMOVED, getCallOptions(), request);
    }

    /**
     */
    public firmament.FirmamentSchedulerOuterClass.TaskSubmittedResponse taskSubmitted(firmament.FirmamentSchedulerOuterClass.TaskDescription request) {
      return blockingUnaryCall(
          getChannel(), METHOD_TASK_SUBMITTED, getCallOptions(), request);
    }

    /**
     */
    public firmament.FirmamentSchedulerOuterClass.TaskUpdatedResponse taskUpdated(firmament.FirmamentSchedulerOuterClass.TaskDescription request) {
      return blockingUnaryCall(
          getChannel(), METHOD_TASK_UPDATED, getCallOptions(), request);
    }

    /**
     */
    public firmament.FirmamentSchedulerOuterClass.NodeAddedResponse nodeAdded(firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor request) {
      return blockingUnaryCall(
          getChannel(), METHOD_NODE_ADDED, getCallOptions(), request);
    }

    /**
     */
    public firmament.FirmamentSchedulerOuterClass.NodeFailedResponse nodeFailed(firmament.FirmamentSchedulerOuterClass.ResourceUID request) {
      return blockingUnaryCall(
          getChannel(), METHOD_NODE_FAILED, getCallOptions(), request);
    }

    /**
     */
    public firmament.FirmamentSchedulerOuterClass.NodeRemovedResponse nodeRemoved(firmament.FirmamentSchedulerOuterClass.ResourceUID request) {
      return blockingUnaryCall(
          getChannel(), METHOD_NODE_REMOVED, getCallOptions(), request);
    }

    /**
     */
    public firmament.FirmamentSchedulerOuterClass.NodeUpdatedResponse nodeUpdated(firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor request) {
      return blockingUnaryCall(
          getChannel(), METHOD_NODE_UPDATED, getCallOptions(), request);
    }

    /**
     */
    public firmament.FirmamentSchedulerOuterClass.TaskStatsResponse addTaskStats(firmament.TaskStatsOuterClass.TaskStats request) {
      return blockingUnaryCall(
          getChannel(), METHOD_ADD_TASK_STATS, getCallOptions(), request);
    }

    /**
     */
    public firmament.FirmamentSchedulerOuterClass.ResourceStatsResponse addNodeStats(firmament.ResourceStatsOuterClass.ResourceStats request) {
      return blockingUnaryCall(
          getChannel(), METHOD_ADD_NODE_STATS, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class FirmamentSchedulerFutureStub extends io.grpc.stub.AbstractStub<FirmamentSchedulerFutureStub> {
    private FirmamentSchedulerFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private FirmamentSchedulerFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FirmamentSchedulerFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new FirmamentSchedulerFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<firmament.FirmamentSchedulerOuterClass.SchedulingDeltas> schedule(
        firmament.FirmamentSchedulerOuterClass.ScheduleRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_SCHEDULE, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<firmament.FirmamentSchedulerOuterClass.TaskCompletedResponse> taskCompleted(
        firmament.FirmamentSchedulerOuterClass.TaskUID request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_TASK_COMPLETED, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<firmament.FirmamentSchedulerOuterClass.TaskFailedResponse> taskFailed(
        firmament.FirmamentSchedulerOuterClass.TaskUID request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_TASK_FAILED, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<firmament.FirmamentSchedulerOuterClass.TaskRemovedResponse> taskRemoved(
        firmament.FirmamentSchedulerOuterClass.TaskUID request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_TASK_REMOVED, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<firmament.FirmamentSchedulerOuterClass.TaskSubmittedResponse> taskSubmitted(
        firmament.FirmamentSchedulerOuterClass.TaskDescription request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_TASK_SUBMITTED, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<firmament.FirmamentSchedulerOuterClass.TaskUpdatedResponse> taskUpdated(
        firmament.FirmamentSchedulerOuterClass.TaskDescription request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_TASK_UPDATED, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<firmament.FirmamentSchedulerOuterClass.NodeAddedResponse> nodeAdded(
        firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_NODE_ADDED, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<firmament.FirmamentSchedulerOuterClass.NodeFailedResponse> nodeFailed(
        firmament.FirmamentSchedulerOuterClass.ResourceUID request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_NODE_FAILED, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<firmament.FirmamentSchedulerOuterClass.NodeRemovedResponse> nodeRemoved(
        firmament.FirmamentSchedulerOuterClass.ResourceUID request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_NODE_REMOVED, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<firmament.FirmamentSchedulerOuterClass.NodeUpdatedResponse> nodeUpdated(
        firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_NODE_UPDATED, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<firmament.FirmamentSchedulerOuterClass.TaskStatsResponse> addTaskStats(
        firmament.TaskStatsOuterClass.TaskStats request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_ADD_TASK_STATS, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<firmament.FirmamentSchedulerOuterClass.ResourceStatsResponse> addNodeStats(
        firmament.ResourceStatsOuterClass.ResourceStats request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_ADD_NODE_STATS, getCallOptions()), request);
    }
  }

  private static final int METHODID_SCHEDULE = 0;
  private static final int METHODID_TASK_COMPLETED = 1;
  private static final int METHODID_TASK_FAILED = 2;
  private static final int METHODID_TASK_REMOVED = 3;
  private static final int METHODID_TASK_SUBMITTED = 4;
  private static final int METHODID_TASK_UPDATED = 5;
  private static final int METHODID_NODE_ADDED = 6;
  private static final int METHODID_NODE_FAILED = 7;
  private static final int METHODID_NODE_REMOVED = 8;
  private static final int METHODID_NODE_UPDATED = 9;
  private static final int METHODID_ADD_TASK_STATS = 10;
  private static final int METHODID_ADD_NODE_STATS = 11;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final FirmamentSchedulerImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(FirmamentSchedulerImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SCHEDULE:
          serviceImpl.schedule((firmament.FirmamentSchedulerOuterClass.ScheduleRequest) request,
              (io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.SchedulingDeltas>) responseObserver);
          break;
        case METHODID_TASK_COMPLETED:
          serviceImpl.taskCompleted((firmament.FirmamentSchedulerOuterClass.TaskUID) request,
              (io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskCompletedResponse>) responseObserver);
          break;
        case METHODID_TASK_FAILED:
          serviceImpl.taskFailed((firmament.FirmamentSchedulerOuterClass.TaskUID) request,
              (io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskFailedResponse>) responseObserver);
          break;
        case METHODID_TASK_REMOVED:
          serviceImpl.taskRemoved((firmament.FirmamentSchedulerOuterClass.TaskUID) request,
              (io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskRemovedResponse>) responseObserver);
          break;
        case METHODID_TASK_SUBMITTED:
          serviceImpl.taskSubmitted((firmament.FirmamentSchedulerOuterClass.TaskDescription) request,
              (io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskSubmittedResponse>) responseObserver);
          break;
        case METHODID_TASK_UPDATED:
          serviceImpl.taskUpdated((firmament.FirmamentSchedulerOuterClass.TaskDescription) request,
              (io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskUpdatedResponse>) responseObserver);
          break;
        case METHODID_NODE_ADDED:
          serviceImpl.nodeAdded((firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor) request,
              (io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.NodeAddedResponse>) responseObserver);
          break;
        case METHODID_NODE_FAILED:
          serviceImpl.nodeFailed((firmament.FirmamentSchedulerOuterClass.ResourceUID) request,
              (io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.NodeFailedResponse>) responseObserver);
          break;
        case METHODID_NODE_REMOVED:
          serviceImpl.nodeRemoved((firmament.FirmamentSchedulerOuterClass.ResourceUID) request,
              (io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.NodeRemovedResponse>) responseObserver);
          break;
        case METHODID_NODE_UPDATED:
          serviceImpl.nodeUpdated((firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor) request,
              (io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.NodeUpdatedResponse>) responseObserver);
          break;
        case METHODID_ADD_TASK_STATS:
          serviceImpl.addTaskStats((firmament.TaskStatsOuterClass.TaskStats) request,
              (io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.TaskStatsResponse>) responseObserver);
          break;
        case METHODID_ADD_NODE_STATS:
          serviceImpl.addNodeStats((firmament.ResourceStatsOuterClass.ResourceStats) request,
              (io.grpc.stub.StreamObserver<firmament.FirmamentSchedulerOuterClass.ResourceStatsResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static final class FirmamentSchedulerDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return firmament.FirmamentSchedulerOuterClass.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (FirmamentSchedulerGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new FirmamentSchedulerDescriptorSupplier())
              .addMethod(METHOD_SCHEDULE)
              .addMethod(METHOD_TASK_COMPLETED)
              .addMethod(METHOD_TASK_FAILED)
              .addMethod(METHOD_TASK_REMOVED)
              .addMethod(METHOD_TASK_SUBMITTED)
              .addMethod(METHOD_TASK_UPDATED)
              .addMethod(METHOD_NODE_ADDED)
              .addMethod(METHOD_NODE_FAILED)
              .addMethod(METHOD_NODE_REMOVED)
              .addMethod(METHOD_NODE_UPDATED)
              .addMethod(METHOD_ADD_TASK_STATS)
              .addMethod(METHOD_ADD_NODE_STATS)
              .build();
        }
      }
    }
    return result;
  }
}
