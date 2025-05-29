package com.whitelabel.android.data.model.proto

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import com.squareup.wire.internal.equals
import okio.ByteString
import java.io.IOException

class RecolorImageColor @JvmOverloads constructor(
    @field:WireField(
        tag = 1,
        adapter = "com.squareup.wire.ProtoAdapter#STRING"
    ) val code: String?, @field:WireField(
        tag = 2,
        adapter = "com.squareup.wire.ProtoAdapter#FIXED32"
    ) val rgb: Int?, @field:WireField(
        tag = 3,
        adapter = "com.squareup.wire.ProtoAdapter#STRING"
    ) val name: String?, @field:WireField(
        tag = 4,
        adapter = "com.squareup.wire.ProtoAdapter#STRING"
    ) val fandeckName: String?, @field:WireField(
        tag = 5,
        adapter = "com.squareup.wire.ProtoAdapter#STRING"
    ) val fandeckId: String?, unknownFields: ByteString = ByteString.EMPTY
) :
    Message<RecolorImageColor, RecolorImageColor.Builder>(ADAPTER, unknownFields) {
    override fun newBuilder(): Builder {
        val builder = Builder()
        builder.code = code
        builder.rgb = rgb
        builder.name = name
        builder.fandeckName = fandeckName
        builder.fandeckId = fandeckId
        builder.addUnknownFields(unknownFields)
        return builder
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecolorImageColor) return false
        return equals(unknownFields, other.unknownFields) &&
                equals(code, other.code) &&
                equals(rgb, other.rgb) &&
                equals(name, other.name) &&
                equals(fandeckName, other.fandeckName) &&
                equals(fandeckId, other.fandeckId)
    }

    override fun hashCode(): Int {
        var result = hashCode
        if (result == 0) {
            result = unknownFields.hashCode()
            result = result * 37 + (code?.hashCode() ?: 0)
            result = result * 37 + (rgb?.hashCode() ?: 0)
            result = result * 37 + (name?.hashCode() ?: 0)
            result = result * 37 + (fandeckName?.hashCode() ?: 0)
            result = result * 37 + (fandeckId?.hashCode() ?: 0)
            hashCode = result
        }
        return result
    }

    override fun toString(): String {
        val sb = StringBuilder()
        if (code != null) sb.append(", code=").append(code)
        if (rgb != null) sb.append(", rgb=").append(rgb)
        if (name != null) sb.append(", name=").append(name)
        if (fandeckName != null) sb.append(", fandeckName=").append(fandeckName)
        if (fandeckId != null) sb.append(", fandeckId=").append(fandeckId)
        return sb.replace(0, 2, "RecolorImageColor{").append('}').toString()
    }

    class Builder : Message.Builder<RecolorImageColor, Builder>() {
        var code: String? = null
        var rgb: Int? = null
        var name: String? = null
        var fandeckName: String? = null
        var fandeckId: String? = null

        fun code(code: String?): Builder {
            this.code = code
            return this
        }

        fun rgb(rgb: Int?): Builder {
            this.rgb = rgb
            return this
        }

        fun name(name: String?): Builder {
            this.name = name
            return this
        }

        fun fandeckName(name: String?): Builder {
            this.fandeckName = name
            return this
        }

        fun fandeckId(id: String?): Builder {
            this.fandeckId = id
            return this
        }

        override fun build(): RecolorImageColor {
            return RecolorImageColor(code, rgb, name, fandeckName, fandeckId, buildUnknownFields())
        }
    }

    private class ProtoAdapterRecolorImageColor :
        ProtoAdapter<RecolorImageColor>(
            FieldEncoding.LENGTH_DELIMITED,
            RecolorImageColor::class.java
        ) {
        override fun encodedSize(value: RecolorImageColor): Int {
            var size = 0
            if (value.code != null) size += STRING.encodedSizeWithTag(1, value.code)
            if (value.rgb != null) size += FIXED32.encodedSizeWithTag(2, value.rgb)
            if (value.name != null) size += STRING.encodedSizeWithTag(3, value.name)
            if (value.fandeckName != null) size += STRING.encodedSizeWithTag(4, value.fandeckName)
            if (value.fandeckId != null) size += STRING.encodedSizeWithTag(5, value.fandeckId)
            return size + value.unknownFields.size
        }

        @Throws(IOException::class)
        override fun encode(writer: ProtoWriter, value: RecolorImageColor) {
            if (value.code != null) STRING.encodeWithTag(writer, 1, value.code)
            if (value.rgb != null) FIXED32.encodeWithTag(writer, 2, value.rgb)
            if (value.name != null) STRING.encodeWithTag(writer, 3, value.name)
            if (value.fandeckName != null) STRING.encodeWithTag(writer, 4, value.fandeckName)
            if (value.fandeckId != null) STRING.encodeWithTag(writer, 5, value.fandeckId)
            writer.writeBytes(value.unknownFields)
        }

        @Throws(IOException::class)
        override fun decode(reader: ProtoReader): RecolorImageColor {
            val builder = Builder()
            val token = reader.beginMessage()
            while (true) {
                val tag = reader.nextTag()
                if (tag == -1) break
                when (tag) {
                    1 -> builder.code(STRING.decode(reader))
                    2 -> builder.rgb(FIXED32.decode(reader))
                    3 -> builder.name(STRING.decode(reader))
                    4 -> builder.fandeckName(STRING.decode(reader))
                    5 -> builder.fandeckId(STRING.decode(reader))
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
            reader.endMessageAndGetUnknownFields(token)
            return builder.build()
        }

        override fun redact(value: RecolorImageColor): RecolorImageColor {
            val builder = value.newBuilder()
            builder.clearUnknownFields()
            return builder.build()
        }
    }

    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<RecolorImageColor> = ProtoAdapterRecolorImageColor()
        private const val serialVersionUID = 0L
    }
}