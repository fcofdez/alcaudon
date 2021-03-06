// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: scheduling_delta.proto

package firmament;

public final class SchedulingDeltaOuterClass {
  private SchedulingDeltaOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public interface SchedulingDeltaOrBuilder extends
      // @@protoc_insertion_point(interface_extends:firmament.SchedulingDelta)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>optional uint64 task_id = 1;</code>
     */
    long getTaskId();

    /**
     * <code>optional string resource_id = 2;</code>
     */
    java.lang.String getResourceId();
    /**
     * <code>optional string resource_id = 2;</code>
     */
    com.google.protobuf.ByteString
        getResourceIdBytes();

    /**
     * <code>optional .firmament.SchedulingDelta.ChangeType type = 3;</code>
     */
    int getTypeValue();
    /**
     * <code>optional .firmament.SchedulingDelta.ChangeType type = 3;</code>
     */
    firmament.SchedulingDeltaOuterClass.SchedulingDelta.ChangeType getType();
  }
  /**
   * Protobuf type {@code firmament.SchedulingDelta}
   */
  public  static final class SchedulingDelta extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:firmament.SchedulingDelta)
      SchedulingDeltaOrBuilder {
    // Use SchedulingDelta.newBuilder() to construct.
    private SchedulingDelta(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }
    private SchedulingDelta() {
      taskId_ = 0L;
      resourceId_ = "";
      type_ = 0;
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
    }
    private SchedulingDelta(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      int mutable_bitField0_ = 0;
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!input.skipField(tag)) {
                done = true;
              }
              break;
            }
            case 8: {

              taskId_ = input.readUInt64();
              break;
            }
            case 18: {
              java.lang.String s = input.readStringRequireUtf8();

              resourceId_ = s;
              break;
            }
            case 24: {
              int rawValue = input.readEnum();

              type_ = rawValue;
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e).setUnfinishedMessage(this);
      } finally {
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return firmament.SchedulingDeltaOuterClass.internal_static_firmament_SchedulingDelta_descriptor;
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return firmament.SchedulingDeltaOuterClass.internal_static_firmament_SchedulingDelta_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              firmament.SchedulingDeltaOuterClass.SchedulingDelta.class, firmament.SchedulingDeltaOuterClass.SchedulingDelta.Builder.class);
    }

    /**
     * Protobuf enum {@code firmament.SchedulingDelta.ChangeType}
     */
    public enum ChangeType
        implements com.google.protobuf.ProtocolMessageEnum {
      /**
       * <code>NOOP = 0;</code>
       */
      NOOP(0),
      /**
       * <code>PLACE = 1;</code>
       */
      PLACE(1),
      /**
       * <code>PREEMPT = 2;</code>
       */
      PREEMPT(2),
      /**
       * <code>MIGRATE = 3;</code>
       */
      MIGRATE(3),
      UNRECOGNIZED(-1),
      ;

      /**
       * <code>NOOP = 0;</code>
       */
      public static final int NOOP_VALUE = 0;
      /**
       * <code>PLACE = 1;</code>
       */
      public static final int PLACE_VALUE = 1;
      /**
       * <code>PREEMPT = 2;</code>
       */
      public static final int PREEMPT_VALUE = 2;
      /**
       * <code>MIGRATE = 3;</code>
       */
      public static final int MIGRATE_VALUE = 3;


      public final int getNumber() {
        if (this == UNRECOGNIZED) {
          throw new java.lang.IllegalArgumentException(
              "Can't get the number of an unknown enum value.");
        }
        return value;
      }

      /**
       * @deprecated Use {@link #forNumber(int)} instead.
       */
      @java.lang.Deprecated
      public static ChangeType valueOf(int value) {
        return forNumber(value);
      }

      public static ChangeType forNumber(int value) {
        switch (value) {
          case 0: return NOOP;
          case 1: return PLACE;
          case 2: return PREEMPT;
          case 3: return MIGRATE;
          default: return null;
        }
      }

      public static com.google.protobuf.Internal.EnumLiteMap<ChangeType>
          internalGetValueMap() {
        return internalValueMap;
      }
      private static final com.google.protobuf.Internal.EnumLiteMap<
          ChangeType> internalValueMap =
            new com.google.protobuf.Internal.EnumLiteMap<ChangeType>() {
              public ChangeType findValueByNumber(int number) {
                return ChangeType.forNumber(number);
              }
            };

      public final com.google.protobuf.Descriptors.EnumValueDescriptor
          getValueDescriptor() {
        return getDescriptor().getValues().get(ordinal());
      }
      public final com.google.protobuf.Descriptors.EnumDescriptor
          getDescriptorForType() {
        return getDescriptor();
      }
      public static final com.google.protobuf.Descriptors.EnumDescriptor
          getDescriptor() {
        return firmament.SchedulingDeltaOuterClass.SchedulingDelta.getDescriptor().getEnumTypes().get(0);
      }

      private static final ChangeType[] VALUES = values();

      public static ChangeType valueOf(
          com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
        if (desc.getType() != getDescriptor()) {
          throw new java.lang.IllegalArgumentException(
            "EnumValueDescriptor is not for this type.");
        }
        if (desc.getIndex() == -1) {
          return UNRECOGNIZED;
        }
        return VALUES[desc.getIndex()];
      }

      private final int value;

      private ChangeType(int value) {
        this.value = value;
      }

      // @@protoc_insertion_point(enum_scope:firmament.SchedulingDelta.ChangeType)
    }

    public static final int TASK_ID_FIELD_NUMBER = 1;
    private long taskId_;
    /**
     * <code>optional uint64 task_id = 1;</code>
     */
    public long getTaskId() {
      return taskId_;
    }

    public static final int RESOURCE_ID_FIELD_NUMBER = 2;
    private volatile java.lang.Object resourceId_;
    /**
     * <code>optional string resource_id = 2;</code>
     */
    public java.lang.String getResourceId() {
      java.lang.Object ref = resourceId_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        resourceId_ = s;
        return s;
      }
    }
    /**
     * <code>optional string resource_id = 2;</code>
     */
    public com.google.protobuf.ByteString
        getResourceIdBytes() {
      java.lang.Object ref = resourceId_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        resourceId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int TYPE_FIELD_NUMBER = 3;
    private int type_;
    /**
     * <code>optional .firmament.SchedulingDelta.ChangeType type = 3;</code>
     */
    public int getTypeValue() {
      return type_;
    }
    /**
     * <code>optional .firmament.SchedulingDelta.ChangeType type = 3;</code>
     */
    public firmament.SchedulingDeltaOuterClass.SchedulingDelta.ChangeType getType() {
      firmament.SchedulingDeltaOuterClass.SchedulingDelta.ChangeType result = firmament.SchedulingDeltaOuterClass.SchedulingDelta.ChangeType.valueOf(type_);
      return result == null ? firmament.SchedulingDeltaOuterClass.SchedulingDelta.ChangeType.UNRECOGNIZED : result;
    }

    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (taskId_ != 0L) {
        output.writeUInt64(1, taskId_);
      }
      if (!getResourceIdBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, resourceId_);
      }
      if (type_ != firmament.SchedulingDeltaOuterClass.SchedulingDelta.ChangeType.NOOP.getNumber()) {
        output.writeEnum(3, type_);
      }
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (taskId_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt64Size(1, taskId_);
      }
      if (!getResourceIdBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, resourceId_);
      }
      if (type_ != firmament.SchedulingDeltaOuterClass.SchedulingDelta.ChangeType.NOOP.getNumber()) {
        size += com.google.protobuf.CodedOutputStream
          .computeEnumSize(3, type_);
      }
      memoizedSize = size;
      return size;
    }

    private static final long serialVersionUID = 0L;
    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof firmament.SchedulingDeltaOuterClass.SchedulingDelta)) {
        return super.equals(obj);
      }
      firmament.SchedulingDeltaOuterClass.SchedulingDelta other = (firmament.SchedulingDeltaOuterClass.SchedulingDelta) obj;

      boolean result = true;
      result = result && (getTaskId()
          == other.getTaskId());
      result = result && getResourceId()
          .equals(other.getResourceId());
      result = result && type_ == other.type_;
      return result;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptorForType().hashCode();
      hash = (37 * hash) + TASK_ID_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getTaskId());
      hash = (37 * hash) + RESOURCE_ID_FIELD_NUMBER;
      hash = (53 * hash) + getResourceId().hashCode();
      hash = (37 * hash) + TYPE_FIELD_NUMBER;
      hash = (53 * hash) + type_;
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static firmament.SchedulingDeltaOuterClass.SchedulingDelta parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static firmament.SchedulingDeltaOuterClass.SchedulingDelta parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static firmament.SchedulingDeltaOuterClass.SchedulingDelta parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static firmament.SchedulingDeltaOuterClass.SchedulingDelta parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static firmament.SchedulingDeltaOuterClass.SchedulingDelta parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static firmament.SchedulingDeltaOuterClass.SchedulingDelta parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static firmament.SchedulingDeltaOuterClass.SchedulingDelta parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static firmament.SchedulingDeltaOuterClass.SchedulingDelta parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static firmament.SchedulingDeltaOuterClass.SchedulingDelta parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static firmament.SchedulingDeltaOuterClass.SchedulingDelta parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(firmament.SchedulingDeltaOuterClass.SchedulingDelta prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code firmament.SchedulingDelta}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:firmament.SchedulingDelta)
        firmament.SchedulingDeltaOuterClass.SchedulingDeltaOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return firmament.SchedulingDeltaOuterClass.internal_static_firmament_SchedulingDelta_descriptor;
      }

      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return firmament.SchedulingDeltaOuterClass.internal_static_firmament_SchedulingDelta_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                firmament.SchedulingDeltaOuterClass.SchedulingDelta.class, firmament.SchedulingDeltaOuterClass.SchedulingDelta.Builder.class);
      }

      // Construct using firmament.SchedulingDeltaOuterClass.SchedulingDelta.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
                .alwaysUseFieldBuilders) {
        }
      }
      public Builder clear() {
        super.clear();
        taskId_ = 0L;

        resourceId_ = "";

        type_ = 0;

        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return firmament.SchedulingDeltaOuterClass.internal_static_firmament_SchedulingDelta_descriptor;
      }

      public firmament.SchedulingDeltaOuterClass.SchedulingDelta getDefaultInstanceForType() {
        return firmament.SchedulingDeltaOuterClass.SchedulingDelta.getDefaultInstance();
      }

      public firmament.SchedulingDeltaOuterClass.SchedulingDelta build() {
        firmament.SchedulingDeltaOuterClass.SchedulingDelta result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public firmament.SchedulingDeltaOuterClass.SchedulingDelta buildPartial() {
        firmament.SchedulingDeltaOuterClass.SchedulingDelta result = new firmament.SchedulingDeltaOuterClass.SchedulingDelta(this);
        result.taskId_ = taskId_;
        result.resourceId_ = resourceId_;
        result.type_ = type_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }
      public Builder setField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.setField(field, value);
      }
      public Builder clearField(
          com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }
      public Builder clearOneof(
          com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          int index, Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof firmament.SchedulingDeltaOuterClass.SchedulingDelta) {
          return mergeFrom((firmament.SchedulingDeltaOuterClass.SchedulingDelta)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(firmament.SchedulingDeltaOuterClass.SchedulingDelta other) {
        if (other == firmament.SchedulingDeltaOuterClass.SchedulingDelta.getDefaultInstance()) return this;
        if (other.getTaskId() != 0L) {
          setTaskId(other.getTaskId());
        }
        if (!other.getResourceId().isEmpty()) {
          resourceId_ = other.resourceId_;
          onChanged();
        }
        if (other.type_ != 0) {
          setTypeValue(other.getTypeValue());
        }
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        firmament.SchedulingDeltaOuterClass.SchedulingDelta parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (firmament.SchedulingDeltaOuterClass.SchedulingDelta) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private long taskId_ ;
      /**
       * <code>optional uint64 task_id = 1;</code>
       */
      public long getTaskId() {
        return taskId_;
      }
      /**
       * <code>optional uint64 task_id = 1;</code>
       */
      public Builder setTaskId(long value) {
        
        taskId_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional uint64 task_id = 1;</code>
       */
      public Builder clearTaskId() {
        
        taskId_ = 0L;
        onChanged();
        return this;
      }

      private java.lang.Object resourceId_ = "";
      /**
       * <code>optional string resource_id = 2;</code>
       */
      public java.lang.String getResourceId() {
        java.lang.Object ref = resourceId_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          resourceId_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>optional string resource_id = 2;</code>
       */
      public com.google.protobuf.ByteString
          getResourceIdBytes() {
        java.lang.Object ref = resourceId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          resourceId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>optional string resource_id = 2;</code>
       */
      public Builder setResourceId(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  
        resourceId_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional string resource_id = 2;</code>
       */
      public Builder clearResourceId() {
        
        resourceId_ = getDefaultInstance().getResourceId();
        onChanged();
        return this;
      }
      /**
       * <code>optional string resource_id = 2;</code>
       */
      public Builder setResourceIdBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
        
        resourceId_ = value;
        onChanged();
        return this;
      }

      private int type_ = 0;
      /**
       * <code>optional .firmament.SchedulingDelta.ChangeType type = 3;</code>
       */
      public int getTypeValue() {
        return type_;
      }
      /**
       * <code>optional .firmament.SchedulingDelta.ChangeType type = 3;</code>
       */
      public Builder setTypeValue(int value) {
        type_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional .firmament.SchedulingDelta.ChangeType type = 3;</code>
       */
      public firmament.SchedulingDeltaOuterClass.SchedulingDelta.ChangeType getType() {
        firmament.SchedulingDeltaOuterClass.SchedulingDelta.ChangeType result = firmament.SchedulingDeltaOuterClass.SchedulingDelta.ChangeType.valueOf(type_);
        return result == null ? firmament.SchedulingDeltaOuterClass.SchedulingDelta.ChangeType.UNRECOGNIZED : result;
      }
      /**
       * <code>optional .firmament.SchedulingDelta.ChangeType type = 3;</code>
       */
      public Builder setType(firmament.SchedulingDeltaOuterClass.SchedulingDelta.ChangeType value) {
        if (value == null) {
          throw new NullPointerException();
        }
        
        type_ = value.getNumber();
        onChanged();
        return this;
      }
      /**
       * <code>optional .firmament.SchedulingDelta.ChangeType type = 3;</code>
       */
      public Builder clearType() {
        
        type_ = 0;
        onChanged();
        return this;
      }
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return this;
      }

      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return this;
      }


      // @@protoc_insertion_point(builder_scope:firmament.SchedulingDelta)
    }

    // @@protoc_insertion_point(class_scope:firmament.SchedulingDelta)
    private static final firmament.SchedulingDeltaOuterClass.SchedulingDelta DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new firmament.SchedulingDeltaOuterClass.SchedulingDelta();
    }

    public static firmament.SchedulingDeltaOuterClass.SchedulingDelta getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<SchedulingDelta>
        PARSER = new com.google.protobuf.AbstractParser<SchedulingDelta>() {
      public SchedulingDelta parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
          return new SchedulingDelta(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<SchedulingDelta> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<SchedulingDelta> getParserForType() {
      return PARSER;
    }

    public firmament.SchedulingDeltaOuterClass.SchedulingDelta getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_firmament_SchedulingDelta_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_firmament_SchedulingDelta_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\026scheduling_delta.proto\022\tfirmament\"\251\001\n\017" +
      "SchedulingDelta\022\017\n\007task_id\030\001 \001(\004\022\023\n\013reso" +
      "urce_id\030\002 \001(\t\0223\n\004type\030\003 \001(\0162%.firmament." +
      "SchedulingDelta.ChangeType\";\n\nChangeType" +
      "\022\010\n\004NOOP\020\000\022\t\n\005PLACE\020\001\022\013\n\007PREEMPT\020\002\022\013\n\007MI" +
      "GRATE\020\003b\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
    internal_static_firmament_SchedulingDelta_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_firmament_SchedulingDelta_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_firmament_SchedulingDelta_descriptor,
        new java.lang.String[] { "TaskId", "ResourceId", "Type", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
