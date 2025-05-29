package com.whitelabel.android.data.model.proto

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import com.squareup.wire.internal.checkElementsNotNull
import com.squareup.wire.internal.copyOf
import com.squareup.wire.internal.equals
import com.squareup.wire.internal.immutableCopyOf
import com.squareup.wire.internal.missingRequiredFields
import com.squareup.wire.internal.newMutableList
import com.squareup.wire.internal.redactElements
import okio.ByteString
import java.io.IOException
import java.util.StringJoiner

class RecolorImageMeta @JvmOverloads constructor(
    @field:WireField(
        adapter = "com.squareup.wire.ProtoAdapter#INT32",
        label = WireField.Label.REQUIRED,
        tag = 1
    ) val version: Int?,
    time: Long?,
    usedColours: List<RecolorImageColor>,
    thumbnail: ByteString?,
    label: String?,
    unknownFields: ByteString = ByteString.EMPTY
) :
    Message<RecolorImageMeta, RecolorImageMeta.Builder>(ADAPTER, unknownFields) {
    @WireField(adapter = "com.squareup.wire.ProtoAdapter#UINT64", tag = 2)
    val time: Long?

    @WireField(
        adapter = "com.whitelabel.proto.model.android.RecolorImageColor#ADAPTER",
        label = WireField.Label.REPEATED,
        tag = 3
    )
    val usedColours: List<RecolorImageColor>

    @WireField(adapter = "com.squareup.wire.ProtoAdapter#BYTES", tag = 4)
    val thumbnail: ByteString?

    @WireField(adapter = "com.squareup.wire.ProtoAdapter#STRING", tag = 5)
    val label: String

    init {
        this.time = time ?: DEFAULT_TIME
        this.usedColours = immutableCopyOf("used_colours", usedColours)
        this.thumbnail = thumbnail ?: DEFAULT_THUMBNAIL
        this.label = label ?: DEFAULT_LABEL
    }

    override fun newBuilder(): Builder {
        val builder = Builder()
        builder.version = version
        builder.time = time
        builder.usedBuilderColours = copyOf(usedColours)
        builder.thumbnail = thumbnail
        builder.label = label
        builder.addUnknownFields(unknownFields)
        return builder
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecolorImageMeta) return false
        return unknownFields == other.unknownFields
                && equals(version, other.version)
                && equals(time, other.time)
                && equals(usedColours, other.usedColours)
                && equals(thumbnail, other.thumbnail)
                && equals(label, other.label)
    }

    override fun hashCode(): Int {
        if (hashCode != 0) return hashCode
        var result = unknownFields.hashCode()
        result = 37 * result + (version?.hashCode() ?: 0)
        result = 37 * result + (time?.hashCode() ?: 0)
        result = 37 * result + usedColours.hashCode()
        result = 37 * result + thumbnail.hashCode()
        result = 37 * result + label.hashCode()
        hashCode = result
        return result
    }

    override fun toString(): String {
        val joiner = StringJoiner(", ", "RecolorImageMeta{", "}")
        joiner.add("version=$version")
        if (time != null) joiner.add("time=$time")
        if (usedColours.isNotEmpty()) joiner.add("used_colours=$usedColours")
        if (thumbnail != ByteString.EMPTY) joiner.add("thumbnail=$thumbnail")
        if (label.isNotEmpty()) joiner.add("label=$label")
        return joiner.toString()
    }

    class Builder : Message.Builder<RecolorImageMeta, Builder>() {
        var version: Int? = null
        var time: Long? = null
        var usedBuilderColours: MutableList<RecolorImageColor> = newMutableList()
        var thumbnail: ByteString? = null
        var label: String? = null

        fun version(version: Int?): Builder {
            this.version = version
            return this
        }

        fun time(time: Long?): Builder {
            this.time = time
            return this
        }

        fun usedColours(colors: MutableList<RecolorImageColor>): Builder {
            checkElementsNotNull(colors)
            this.usedBuilderColours = colors
            return this
        }

        fun thumbnail(thumbnail: ByteString?): Builder {
            this.thumbnail = thumbnail
            return this
        }

        fun label(label: String?): Builder {
            this.label = label
            return this
        }

        override fun build(): RecolorImageMeta {
            if (version == null) {
                throw missingRequiredFields(version, "version")
            }
            return RecolorImageMeta(
                version,
                time,
                usedBuilderColours,
                thumbnail,
                label,
                buildUnknownFields()
            )
        }
    }

    private class ProtoAdapterRecolorImageMeta :
        ProtoAdapter<RecolorImageMeta>(
            FieldEncoding.LENGTH_DELIMITED,
            RecolorImageMeta::class.java
        ) {
        override fun encodedSize(value: RecolorImageMeta): Int {
            return (INT32.encodedSizeWithTag(1, value.version)
                    + (if (value.time != null) UINT64.encodedSizeWithTag(2, value.time) else 0)
                    + RecolorImageColor.ADAPTER.asRepeated()
                .encodedSizeWithTag(3, value.usedColours)
                    + (if (value.thumbnail != null) BYTES.encodedSizeWithTag(
                4,
                value.thumbnail
            ) else 0)
                    + (STRING.encodedSizeWithTag(5, value.label))
                    + value.unknownFields.size)
        }

        @Throws(IOException::class)
        override fun encode(writer: ProtoWriter, value: RecolorImageMeta) {
            INT32.encodeWithTag(writer, 1, value.version)
            if (value.time != null) UINT64.encodeWithTag(writer, 2, value.time)
            RecolorImageColor.ADAPTER.asRepeated().encodeWithTag(writer, 3, value.usedColours)
            if (value.thumbnail != null) BYTES.encodeWithTag(writer, 4, value.thumbnail)
            STRING.encodeWithTag(writer, 5, value.label)
            writer.writeBytes(value.unknownFields)
        }

        @Throws(IOException::class)
        override fun decode(reader: ProtoReader): RecolorImageMeta {
            val builder = Builder()
            val token = reader.beginMessage()
            while (true) {
                val tag = reader.nextTag()
                if (tag == -1) {
                    reader.endMessageAndGetUnknownFields(token)
                    return builder.build()
                }
                when (tag) {
                    1 -> builder.version(INT32.decode(reader))
                    2 -> builder.time(UINT64.decode(reader))
                    3 -> builder.usedBuilderColours.add(RecolorImageColor.ADAPTER.decode(reader))
                    4 -> builder.thumbnail(BYTES.decode(reader))
                    5 -> builder.label(STRING.decode(reader))
                    else -> {
                        val encoding = reader.peekFieldEncoding()
                        builder.addUnknownField(
                            tag,
                            encoding!!,
                            encoding.rawProtoAdapter().decode(reader)
                        )
                    }
                }
            }
        }

        override fun redact(value: RecolorImageMeta): RecolorImageMeta {
            val builder = value.newBuilder()
            redactElements(builder.usedBuilderColours, RecolorImageColor.ADAPTER)
            builder.clearUnknownFields()
            return builder.build()
        }
    }

    companion object {
        const val DEFAULT_LABEL: String = ""
        const val DEFAULT_TIME: Long = 0L
        val DEFAULT_THUMBNAIL: ByteString = ByteString.EMPTY
        const val DEFAULT_VERSION: Int = 0

        private const val serialVersionUID = 0L

        val ADAPTER: ProtoAdapter<RecolorImageMeta> = ProtoAdapterRecolorImageMeta()
    }
} /*

public final class RecolorImageMeta extends Message<RecolorImageMeta, RecolorImage.Builder> {
    public static final String DEFAULT_LABEL = "";
    private static final long serialVersionUID = 0;
    @WireField(adapter = "com.squareup.wire.ProtoAdapter#STRING", tag = 5)
    public final String label;
    @WireField(adapter = "com.squareup.wire.ProtoAdapter#BYTES", tag = 4)
    public final ByteString thumbnail;
    @WireField(adapter = "com.squareup.wire.ProtoAdapter#UINT64", tag = 2)
    public final Long time;
    @WireField(adapter = "com.whitelabel.proto.model.android.RecolorImageColor#ADAPTER", label = WireField.Label.REPEATED, tag = 3)
    public final List<RecolorImageColor> used_colours;
    @WireField(adapter = "com.squareup.wire.ProtoAdapter#INT32", label = WireField.Label.REQUIRED, tag = 1)
    public final Integer version;
    public static final ProtoAdapter<RecolorImageMeta> ADAPTER = new ProtoAdapter_RecolorImageMeta();
    public static final Integer DEFAULT_VERSION = 0;
    public static final Long DEFAULT_TIME = 0L;
    public static final ByteString DEFAULT_THUMBNAIL = ByteString.EMPTY;

    public RecolorImageMeta(Integer num, Long l, List<RecolorImageColor> list, ByteString byteString, String str) {
        this(num, l, list, byteString, str, ByteString.EMPTY);
    }

    public RecolorImageMeta(Integer num, Long l, List<RecolorImageColor> list, ByteString byteString, String str, ByteString byteString2) {
        super(ADAPTER, byteString2);
        this.version = num;
        this.time = l;
        this.used_colours = Internal.immutableCopyOf("used_colours", list);
        this.thumbnail = byteString;
        this.label = str;
    }

    @Override // com.squareup.wire.Message
    public Builder newBuilder() {
        Builder builder = new Builder();
        builder.version = this.version;
        builder.time = this.time;
        builder.used_colours = Internal.copyOf(this.used_colours);
        builder.thumbnail = this.thumbnail;
        builder.label = this.label;
        builder.addUnknownFields(unknownFields());
        return builder;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof RecolorImageMeta) {
            RecolorImageMeta recolorImageMeta = (RecolorImageMeta) obj;
            return Internal.equals(unknownFields(), recolorImageMeta.unknownFields()) && Internal.equals(this.version, recolorImageMeta.version) && Internal.equals(this.time, recolorImageMeta.time) && Internal.equals(this.used_colours, recolorImageMeta.used_colours) && Internal.equals(this.thumbnail, recolorImageMeta.thumbnail) && Internal.equals(this.label, recolorImageMeta.label);
        }
        return false;
    }

    public int hashCode() {
        int i = this.hashCode;
        if (i == 0) {
            int hashCode = unknownFields().hashCode() * 37;
            Integer num = this.version;
            int hashCode2 = (hashCode + (num != null ? num.hashCode() : 0)) * 37;
            Long l = this.time;
            int hashCode3 = (hashCode2 + (l != null ? l.hashCode() : 0)) * 37;
            List<RecolorImageColor> list = this.used_colours;
            int hashCode4 = (hashCode3 + (list != null ? list.hashCode() : 1)) * 37;
            ByteString byteString = this.thumbnail;
            int hashCode5 = (hashCode4 + (byteString != null ? byteString.hashCode() : 0)) * 37;
            String str = this.label;
            int hashCode6 = hashCode5 + (str != null ? str.hashCode() : 0);
            this.hashCode = hashCode6;
            return hashCode6;
        }
        return i;
    }

    @Override // com.squareup.wire.Message
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.version != null) {
            sb.append(", version=");
            sb.append(this.version);
        }
        if (this.time != null) {
            sb.append(", time=");
            sb.append(this.time);
        }
        if (this.used_colours != null) {
            sb.append(", used_colours=");
            sb.append(this.used_colours);
        }
        if (this.thumbnail != null) {
            sb.append(", thumbnail=");
            sb.append(this.thumbnail);
        }
        if (this.label != null) {
            sb.append(", label=");
            sb.append(this.label);
        }
        StringBuilder replace = sb.replace(0, 2, "RecolorImageMeta{");
        replace.append('}');
        return replace.toString();
    }

    */
/* loaded from: /storage/emulated/0/Documents/jadec/sources/com.whitelabel.upgcolor/dex-files/3.dex */ /*

    public static final class Builder extends Message.Builder<RecolorImageMeta, Builder> {
        public String label;
        public ByteString thumbnail;
        public Long time;
        public List<RecolorImageColor> used_colours = Internal.newMutableList();
        public Integer version;

        public Builder version(Integer num) {
            this.version = num;
            return this;
        }

        public Builder time(Long l) {
            this.time = l;
            return this;
        }

        public Builder used_colours(List<RecolorImageColor> list) {
            Internal.checkElementsNotNull(list);
            this.used_colours = list;
            return this;
        }

        public Builder thumbnail(ByteString byteString) {
            this.thumbnail = byteString;
            return this;
        }

        public Builder label(String str) {
            this.label = str;
            return this;
        }

        @Override // com.squareup.wire.Message.Builder
        public RecolorImageMeta build() {
            Integer num = this.version;
            if (num == null) {
                throw Internal.missingRequiredFields(num, "version");
            }
            return new RecolorImageMeta(this.version, this.time, this.used_colours, this.thumbnail, this.label, buildUnknownFields());
        }
    }

    */
/* loaded from: /storage/emulated/0/Documents/jadec/sources/com.whitelabel.upgcolor/dex-files/3.dex */ /*

    private static final class ProtoAdapter_RecolorImageMeta extends ProtoAdapter<RecolorImageMeta> {
        ProtoAdapter_RecolorImageMeta() {
            super(FieldEncoding.LENGTH_DELIMITED, RecolorImageMeta.class);
        }

        @Override // com.squareup.wire.ProtoAdapter
        public int encodedSize(RecolorImageMeta recolorImageMeta) {
            return ProtoAdapter.INT32.encodedSizeWithTag(1, recolorImageMeta.version) + (recolorImageMeta.time != null ? ProtoAdapter.UINT64.encodedSizeWithTag(2, recolorImageMeta.time) : 0) + RecolorImageColor.ADAPTER.asRepeated().encodedSizeWithTag(3, recolorImageMeta.used_colours) + (recolorImageMeta.thumbnail != null ? ProtoAdapter.BYTES.encodedSizeWithTag(4, recolorImageMeta.thumbnail) : 0) + (recolorImageMeta.label != null ? ProtoAdapter.STRING.encodedSizeWithTag(5, recolorImageMeta.label) : 0) + recolorImageMeta.unknownFields().size();
        }

        @Override // com.squareup.wire.ProtoAdapter
        public void encode(ProtoWriter protoWriter, RecolorImageMeta recolorImageMeta) throws IOException {
            ProtoAdapter.INT32.encodeWithTag(protoWriter, 1, (int) recolorImageMeta.version);
            if (recolorImageMeta.time != null) {
                ProtoAdapter.UINT64.encodeWithTag(protoWriter, 2, (int) recolorImageMeta.time);
            }
            if (recolorImageMeta.used_colours != null) {
                RecolorImageColor.ADAPTER.asRepeated().encodeWithTag(protoWriter, 3, (int) recolorImageMeta.used_colours);
            }
            if (recolorImageMeta.thumbnail != null) {
                ProtoAdapter.BYTES.encodeWithTag(protoWriter, 4, (int) recolorImageMeta.thumbnail);
            }
            if (recolorImageMeta.label != null) {
                ProtoAdapter.STRING.encodeWithTag(protoWriter, 5, (int) recolorImageMeta.label);
            }
            protoWriter.writeBytes(recolorImageMeta.unknownFields());
        }

        */
/* JADX WARN: Can't rename method to resolve collision */ /*

        @Override // com.squareup.wire.ProtoAdapter
        public RecolorImageMeta decode(ProtoReader protoReader) throws IOException {
            Builder builder = new Builder();
            long beginMessage = protoReader.beginMessage();
            while (true) {
                int nextTag = protoReader.nextTag();
                if (nextTag == -1) {
                    protoReader.endMessageAndGetUnknownFields(beginMessage);
                    return builder.build();
                } else if (nextTag == 1) {
                    builder.version(ProtoAdapter.INT32.decode(protoReader));
                } else if (nextTag == 2) {
                    builder.time(ProtoAdapter.UINT64.decode(protoReader));
                } else if (nextTag == 3) {
                    builder.used_colours.add(RecolorImageColor.ADAPTER.decode(protoReader));
                } else if (nextTag == 4) {
                    builder.thumbnail(ProtoAdapter.BYTES.decode(protoReader));
                } else if (nextTag == 5) {
                    builder.label(ProtoAdapter.STRING.decode(protoReader));
                } else {
                    FieldEncoding peekFieldEncoding = protoReader.peekFieldEncoding();
                    builder.addUnknownField(nextTag, peekFieldEncoding, peekFieldEncoding.rawProtoAdapter().decode(protoReader));
                }
            }
        }

        @Override // com.squareup.wire.ProtoAdapter
        public RecolorImageMeta redact(RecolorImageMeta recolorImageMeta) {
            Builder newBuilder = recolorImageMeta.newBuilder();
            Internal.redactElements(newBuilder.used_colours, RecolorImageColor.ADAPTER);
            newBuilder.clearUnknownFields();
            return newBuilder.build();
        }
    }
}
*/
