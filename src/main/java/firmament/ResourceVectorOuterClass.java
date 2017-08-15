// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: resource_vector.proto

package firmament;

public final class ResourceVectorOuterClass {
  private ResourceVectorOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public interface ResourceVectorOrBuilder extends
      // @@protoc_insertion_point(interface_extends:firmament.ResourceVector)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>optional float cpu_cores = 1;</code>
     */
    float getCpuCores();

    /**
     * <code>optional uint64 ram_bw = 2;</code>
     */
    long getRamBw();

    /**
     * <pre>
     * in MB
     * </pre>
     *
     * <code>optional uint64 ram_cap = 3;</code>
     */
    long getRamCap();

    /**
     * <code>optional uint64 disk_bw = 4;</code>
     */
    long getDiskBw();

    /**
     * <code>optional uint64 disk_cap = 5;</code>
     */
    long getDiskCap();

    /**
     * <code>optional uint64 net_tx_bw = 6;</code>
     */
    long getNetTxBw();

    /**
     * <code>optional uint64 net_rx_bw = 7;</code>
     */
    long getNetRxBw();
  }
  /**
   * Protobuf type {@code firmament.ResourceVector}
   */
  public  static final class ResourceVector extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:firmament.ResourceVector)
      ResourceVectorOrBuilder {
    // Use ResourceVector.newBuilder() to construct.
    private ResourceVector(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }
    private ResourceVector() {
      cpuCores_ = 0F;
      ramBw_ = 0L;
      ramCap_ = 0L;
      diskBw_ = 0L;
      diskCap_ = 0L;
      netTxBw_ = 0L;
      netRxBw_ = 0L;
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
    }
    private ResourceVector(
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
            case 13: {

              cpuCores_ = input.readFloat();
              break;
            }
            case 16: {

              ramBw_ = input.readUInt64();
              break;
            }
            case 24: {

              ramCap_ = input.readUInt64();
              break;
            }
            case 32: {

              diskBw_ = input.readUInt64();
              break;
            }
            case 40: {

              diskCap_ = input.readUInt64();
              break;
            }
            case 48: {

              netTxBw_ = input.readUInt64();
              break;
            }
            case 56: {

              netRxBw_ = input.readUInt64();
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
      return firmament.ResourceVectorOuterClass.internal_static_firmament_ResourceVector_descriptor;
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return firmament.ResourceVectorOuterClass.internal_static_firmament_ResourceVector_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              firmament.ResourceVectorOuterClass.ResourceVector.class, firmament.ResourceVectorOuterClass.ResourceVector.Builder.class);
    }

    public static final int CPU_CORES_FIELD_NUMBER = 1;
    private float cpuCores_;
    /**
     * <code>optional float cpu_cores = 1;</code>
     */
    public float getCpuCores() {
      return cpuCores_;
    }

    public static final int RAM_BW_FIELD_NUMBER = 2;
    private long ramBw_;
    /**
     * <code>optional uint64 ram_bw = 2;</code>
     */
    public long getRamBw() {
      return ramBw_;
    }

    public static final int RAM_CAP_FIELD_NUMBER = 3;
    private long ramCap_;
    /**
     * <pre>
     * in MB
     * </pre>
     *
     * <code>optional uint64 ram_cap = 3;</code>
     */
    public long getRamCap() {
      return ramCap_;
    }

    public static final int DISK_BW_FIELD_NUMBER = 4;
    private long diskBw_;
    /**
     * <code>optional uint64 disk_bw = 4;</code>
     */
    public long getDiskBw() {
      return diskBw_;
    }

    public static final int DISK_CAP_FIELD_NUMBER = 5;
    private long diskCap_;
    /**
     * <code>optional uint64 disk_cap = 5;</code>
     */
    public long getDiskCap() {
      return diskCap_;
    }

    public static final int NET_TX_BW_FIELD_NUMBER = 6;
    private long netTxBw_;
    /**
     * <code>optional uint64 net_tx_bw = 6;</code>
     */
    public long getNetTxBw() {
      return netTxBw_;
    }

    public static final int NET_RX_BW_FIELD_NUMBER = 7;
    private long netRxBw_;
    /**
     * <code>optional uint64 net_rx_bw = 7;</code>
     */
    public long getNetRxBw() {
      return netRxBw_;
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
      if (cpuCores_ != 0F) {
        output.writeFloat(1, cpuCores_);
      }
      if (ramBw_ != 0L) {
        output.writeUInt64(2, ramBw_);
      }
      if (ramCap_ != 0L) {
        output.writeUInt64(3, ramCap_);
      }
      if (diskBw_ != 0L) {
        output.writeUInt64(4, diskBw_);
      }
      if (diskCap_ != 0L) {
        output.writeUInt64(5, diskCap_);
      }
      if (netTxBw_ != 0L) {
        output.writeUInt64(6, netTxBw_);
      }
      if (netRxBw_ != 0L) {
        output.writeUInt64(7, netRxBw_);
      }
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (cpuCores_ != 0F) {
        size += com.google.protobuf.CodedOutputStream
          .computeFloatSize(1, cpuCores_);
      }
      if (ramBw_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt64Size(2, ramBw_);
      }
      if (ramCap_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt64Size(3, ramCap_);
      }
      if (diskBw_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt64Size(4, diskBw_);
      }
      if (diskCap_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt64Size(5, diskCap_);
      }
      if (netTxBw_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt64Size(6, netTxBw_);
      }
      if (netRxBw_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt64Size(7, netRxBw_);
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
      if (!(obj instanceof firmament.ResourceVectorOuterClass.ResourceVector)) {
        return super.equals(obj);
      }
      firmament.ResourceVectorOuterClass.ResourceVector other = (firmament.ResourceVectorOuterClass.ResourceVector) obj;

      boolean result = true;
      result = result && (
          java.lang.Float.floatToIntBits(getCpuCores())
          == java.lang.Float.floatToIntBits(
              other.getCpuCores()));
      result = result && (getRamBw()
          == other.getRamBw());
      result = result && (getRamCap()
          == other.getRamCap());
      result = result && (getDiskBw()
          == other.getDiskBw());
      result = result && (getDiskCap()
          == other.getDiskCap());
      result = result && (getNetTxBw()
          == other.getNetTxBw());
      result = result && (getNetRxBw()
          == other.getNetRxBw());
      return result;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptorForType().hashCode();
      hash = (37 * hash) + CPU_CORES_FIELD_NUMBER;
      hash = (53 * hash) + java.lang.Float.floatToIntBits(
          getCpuCores());
      hash = (37 * hash) + RAM_BW_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getRamBw());
      hash = (37 * hash) + RAM_CAP_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getRamCap());
      hash = (37 * hash) + DISK_BW_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getDiskBw());
      hash = (37 * hash) + DISK_CAP_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getDiskCap());
      hash = (37 * hash) + NET_TX_BW_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getNetTxBw());
      hash = (37 * hash) + NET_RX_BW_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getNetRxBw());
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static firmament.ResourceVectorOuterClass.ResourceVector parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static firmament.ResourceVectorOuterClass.ResourceVector parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static firmament.ResourceVectorOuterClass.ResourceVector parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static firmament.ResourceVectorOuterClass.ResourceVector parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static firmament.ResourceVectorOuterClass.ResourceVector parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static firmament.ResourceVectorOuterClass.ResourceVector parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static firmament.ResourceVectorOuterClass.ResourceVector parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static firmament.ResourceVectorOuterClass.ResourceVector parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static firmament.ResourceVectorOuterClass.ResourceVector parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static firmament.ResourceVectorOuterClass.ResourceVector parseFrom(
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
    public static Builder newBuilder(firmament.ResourceVectorOuterClass.ResourceVector prototype) {
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
     * Protobuf type {@code firmament.ResourceVector}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:firmament.ResourceVector)
        firmament.ResourceVectorOuterClass.ResourceVectorOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return firmament.ResourceVectorOuterClass.internal_static_firmament_ResourceVector_descriptor;
      }

      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return firmament.ResourceVectorOuterClass.internal_static_firmament_ResourceVector_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                firmament.ResourceVectorOuterClass.ResourceVector.class, firmament.ResourceVectorOuterClass.ResourceVector.Builder.class);
      }

      // Construct using firmament.ResourceVectorOuterClass.ResourceVector.newBuilder()
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
        cpuCores_ = 0F;

        ramBw_ = 0L;

        ramCap_ = 0L;

        diskBw_ = 0L;

        diskCap_ = 0L;

        netTxBw_ = 0L;

        netRxBw_ = 0L;

        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return firmament.ResourceVectorOuterClass.internal_static_firmament_ResourceVector_descriptor;
      }

      public firmament.ResourceVectorOuterClass.ResourceVector getDefaultInstanceForType() {
        return firmament.ResourceVectorOuterClass.ResourceVector.getDefaultInstance();
      }

      public firmament.ResourceVectorOuterClass.ResourceVector build() {
        firmament.ResourceVectorOuterClass.ResourceVector result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public firmament.ResourceVectorOuterClass.ResourceVector buildPartial() {
        firmament.ResourceVectorOuterClass.ResourceVector result = new firmament.ResourceVectorOuterClass.ResourceVector(this);
        result.cpuCores_ = cpuCores_;
        result.ramBw_ = ramBw_;
        result.ramCap_ = ramCap_;
        result.diskBw_ = diskBw_;
        result.diskCap_ = diskCap_;
        result.netTxBw_ = netTxBw_;
        result.netRxBw_ = netRxBw_;
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
        if (other instanceof firmament.ResourceVectorOuterClass.ResourceVector) {
          return mergeFrom((firmament.ResourceVectorOuterClass.ResourceVector)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(firmament.ResourceVectorOuterClass.ResourceVector other) {
        if (other == firmament.ResourceVectorOuterClass.ResourceVector.getDefaultInstance()) return this;
        if (other.getCpuCores() != 0F) {
          setCpuCores(other.getCpuCores());
        }
        if (other.getRamBw() != 0L) {
          setRamBw(other.getRamBw());
        }
        if (other.getRamCap() != 0L) {
          setRamCap(other.getRamCap());
        }
        if (other.getDiskBw() != 0L) {
          setDiskBw(other.getDiskBw());
        }
        if (other.getDiskCap() != 0L) {
          setDiskCap(other.getDiskCap());
        }
        if (other.getNetTxBw() != 0L) {
          setNetTxBw(other.getNetTxBw());
        }
        if (other.getNetRxBw() != 0L) {
          setNetRxBw(other.getNetRxBw());
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
        firmament.ResourceVectorOuterClass.ResourceVector parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (firmament.ResourceVectorOuterClass.ResourceVector) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private float cpuCores_ ;
      /**
       * <code>optional float cpu_cores = 1;</code>
       */
      public float getCpuCores() {
        return cpuCores_;
      }
      /**
       * <code>optional float cpu_cores = 1;</code>
       */
      public Builder setCpuCores(float value) {
        
        cpuCores_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional float cpu_cores = 1;</code>
       */
      public Builder clearCpuCores() {
        
        cpuCores_ = 0F;
        onChanged();
        return this;
      }

      private long ramBw_ ;
      /**
       * <code>optional uint64 ram_bw = 2;</code>
       */
      public long getRamBw() {
        return ramBw_;
      }
      /**
       * <code>optional uint64 ram_bw = 2;</code>
       */
      public Builder setRamBw(long value) {
        
        ramBw_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional uint64 ram_bw = 2;</code>
       */
      public Builder clearRamBw() {
        
        ramBw_ = 0L;
        onChanged();
        return this;
      }

      private long ramCap_ ;
      /**
       * <pre>
       * in MB
       * </pre>
       *
       * <code>optional uint64 ram_cap = 3;</code>
       */
      public long getRamCap() {
        return ramCap_;
      }
      /**
       * <pre>
       * in MB
       * </pre>
       *
       * <code>optional uint64 ram_cap = 3;</code>
       */
      public Builder setRamCap(long value) {
        
        ramCap_ = value;
        onChanged();
        return this;
      }
      /**
       * <pre>
       * in MB
       * </pre>
       *
       * <code>optional uint64 ram_cap = 3;</code>
       */
      public Builder clearRamCap() {
        
        ramCap_ = 0L;
        onChanged();
        return this;
      }

      private long diskBw_ ;
      /**
       * <code>optional uint64 disk_bw = 4;</code>
       */
      public long getDiskBw() {
        return diskBw_;
      }
      /**
       * <code>optional uint64 disk_bw = 4;</code>
       */
      public Builder setDiskBw(long value) {
        
        diskBw_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional uint64 disk_bw = 4;</code>
       */
      public Builder clearDiskBw() {
        
        diskBw_ = 0L;
        onChanged();
        return this;
      }

      private long diskCap_ ;
      /**
       * <code>optional uint64 disk_cap = 5;</code>
       */
      public long getDiskCap() {
        return diskCap_;
      }
      /**
       * <code>optional uint64 disk_cap = 5;</code>
       */
      public Builder setDiskCap(long value) {
        
        diskCap_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional uint64 disk_cap = 5;</code>
       */
      public Builder clearDiskCap() {
        
        diskCap_ = 0L;
        onChanged();
        return this;
      }

      private long netTxBw_ ;
      /**
       * <code>optional uint64 net_tx_bw = 6;</code>
       */
      public long getNetTxBw() {
        return netTxBw_;
      }
      /**
       * <code>optional uint64 net_tx_bw = 6;</code>
       */
      public Builder setNetTxBw(long value) {
        
        netTxBw_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional uint64 net_tx_bw = 6;</code>
       */
      public Builder clearNetTxBw() {
        
        netTxBw_ = 0L;
        onChanged();
        return this;
      }

      private long netRxBw_ ;
      /**
       * <code>optional uint64 net_rx_bw = 7;</code>
       */
      public long getNetRxBw() {
        return netRxBw_;
      }
      /**
       * <code>optional uint64 net_rx_bw = 7;</code>
       */
      public Builder setNetRxBw(long value) {
        
        netRxBw_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional uint64 net_rx_bw = 7;</code>
       */
      public Builder clearNetRxBw() {
        
        netRxBw_ = 0L;
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


      // @@protoc_insertion_point(builder_scope:firmament.ResourceVector)
    }

    // @@protoc_insertion_point(class_scope:firmament.ResourceVector)
    private static final firmament.ResourceVectorOuterClass.ResourceVector DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new firmament.ResourceVectorOuterClass.ResourceVector();
    }

    public static firmament.ResourceVectorOuterClass.ResourceVector getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<ResourceVector>
        PARSER = new com.google.protobuf.AbstractParser<ResourceVector>() {
      public ResourceVector parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
          return new ResourceVector(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<ResourceVector> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<ResourceVector> getParserForType() {
      return PARSER;
    }

    public firmament.ResourceVectorOuterClass.ResourceVector getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_firmament_ResourceVector_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_firmament_ResourceVector_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\025resource_vector.proto\022\tfirmament\"\215\001\n\016R" +
      "esourceVector\022\021\n\tcpu_cores\030\001 \001(\002\022\016\n\006ram_" +
      "bw\030\002 \001(\004\022\017\n\007ram_cap\030\003 \001(\004\022\017\n\007disk_bw\030\004 \001" +
      "(\004\022\020\n\010disk_cap\030\005 \001(\004\022\021\n\tnet_tx_bw\030\006 \001(\004\022" +
      "\021\n\tnet_rx_bw\030\007 \001(\004b\006proto3"
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
    internal_static_firmament_ResourceVector_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_firmament_ResourceVector_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_firmament_ResourceVector_descriptor,
        new java.lang.String[] { "CpuCores", "RamBw", "RamCap", "DiskBw", "DiskCap", "NetTxBw", "NetRxBw", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
