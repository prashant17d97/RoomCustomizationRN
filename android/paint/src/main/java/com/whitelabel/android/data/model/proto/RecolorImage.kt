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

class RecolorImage @JvmOverloads constructor(
    @field:WireField(
        adapter = "com.squareup.wire.ProtoAdapter#INT32",
        label = WireField.Label.REQUIRED,
        tag = 1
    ) val version: Int?,
    @field:WireField(
        adapter = "com.squareup.wire.ProtoAdapter#BYTES",
        tag = 2
    ) val imageOriginal: ByteString?,
    @field:WireField(
        adapter = "com.squareup.wire.ProtoAdapter#BYTES",
        tag = 3
    ) val imageRecolored: ByteString?,
    list: List<RecolorImageColor>,
    byteString3: ByteString = ByteString.EMPTY
) : Message<RecolorImage, RecolorImage.Builder>(ADAPTER, byteString3) {

    @WireField(
        adapter = "com.whitelabel.proto.model.android.RecolorImageColor#ADAPTER",
        label = WireField.Label.REPEATED,
        tag = 4
    )
    val usedColours: List<RecolorImageColor> = immutableCopyOf("used_colours", list)

    override fun newBuilder(): Builder {
        val builder = Builder()
        builder.version = this.version
        builder.imageOriginal = this.imageOriginal
        builder.imageRecolored = this.imageRecolored
        builder.usedBuilderColours = copyOf(this.usedColours)
        builder.addUnknownFields(unknownFields)
        return builder
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is RecolorImage) {
            return equals(unknownFields, other.unknownFields) && equals(
                this.version,
                other.version
            ) && equals(
                this.imageOriginal, other.imageOriginal
            ) && equals(this.imageRecolored, other.imageRecolored) && equals(
                this.usedColours, other.usedColours
            )
        }
        return false
    }

    override fun hashCode(): Int {
        val i = this.hashCode
        if (i == 0) {
            val hashCode = unknownFields.hashCode() * 37
            val num = this.version
            val hashCode2 = (hashCode + (num?.hashCode() ?: 0)) * 37
            val byteString = this.imageOriginal
            val hashCode3 = (hashCode2 + (byteString?.hashCode() ?: 0)) * 37
            val byteString2 = this.imageRecolored
            val hashCode4 = (hashCode3 + (byteString2?.hashCode() ?: 0)) * 37
            val list = this.usedColours
            val hashCode5 = hashCode4 + list.hashCode()
            this.hashCode = hashCode5
            return hashCode5
        }
        return i
    }

    // com.squareup.wire.Message
    override fun toString(): String {
        val sb = StringBuilder()
        if (this.version != null) {
            sb.append(", version=")
            sb.append(this.version)
        }
        if (this.imageOriginal != null) {
            sb.append(", image_original=")
            sb.append(this.imageOriginal)
        }
        if (this.imageRecolored != null) {
            sb.append(", image_recolored=")
            sb.append(this.imageRecolored)
        }
        sb.append(", used_colours=")
        sb.append(this.usedColours)
        val replace = sb.replace(0, 2, "RecolorImage{")
        replace.append('}')
        return replace.toString()
    }

    class Builder : Message.Builder<RecolorImage, Builder>() {
        var imageOriginal: ByteString? = null
        var imageRecolored: ByteString? = null
        var usedBuilderColours: MutableList<RecolorImageColor> = newMutableList()
        var version: Int? = null

        fun version(num: Int?): Builder {
            this.version = num
            return this
        }

        fun imageOriginal(byteString: ByteString?): Builder {
            this.imageOriginal = byteString
            return this
        }

        fun imageRecolored(byteString: ByteString?): Builder {
            this.imageRecolored = byteString
            return this
        }

        fun usedColours(list: MutableList<RecolorImageColor>): Builder {
            checkElementsNotNull(list)
            this.usedBuilderColours = list
            return this
        }

        // com.squareup.wire.Message.Builder
        override fun build(): RecolorImage {
            val num = this.version
            if (num == null) {
                throw missingRequiredFields(num, "version")
            }
            return RecolorImage(
                this.version,
                this.imageOriginal,
                this.imageRecolored,
                this.usedBuilderColours, buildUnknownFields()
            )
        }
    }

    private class ProtoAdapterRecolorImage :
        ProtoAdapter<RecolorImage>(FieldEncoding.LENGTH_DELIMITED, RecolorImage::class.java) {
        override fun encodedSize(value: RecolorImage): Int {
            return INT32.encodedSizeWithTag(
                1,
                value.version
            ) + (if (value.imageOriginal != null) BYTES.encodedSizeWithTag(
                2,
                value.imageOriginal
            ) else 0) + (if (value.imageRecolored != null) BYTES.encodedSizeWithTag(
                3,
                value.imageRecolored
            ) else 0) + RecolorImageColor.ADAPTER.asRepeated()
                .encodedSizeWithTag(4, value.usedColours) + value.unknownFields.size
        }

        @Throws(IOException::class)
        override fun encode(writer: ProtoWriter, value: RecolorImage) {
            INT32.encodeWithTag(writer, 1, value.version)

            if (value.imageOriginal != null) {
                BYTES.encodeWithTag(writer, 2, value.imageOriginal)
            }
            if (value.imageRecolored != null) {
                BYTES.encodeWithTag(writer, 3, value.imageRecolored)
            }
            RecolorImageColor.ADAPTER.asRepeated()
                .encodeWithTag(writer, 4, value.usedColours)

            writer.writeBytes(value.unknownFields)
        }


        @Throws(IOException::class)  // com.squareup.wire.ProtoAdapter
        override fun decode(reader: ProtoReader): RecolorImage {
            val builder = Builder()
            val beginMessage = reader.beginMessage()
            while (true) {
                when (val nextTag = reader.nextTag()) {
                    -1 -> {
                        reader.endMessageAndGetUnknownFields(beginMessage)
                        return builder.build()
                    }
                    1 -> {
                        builder.version(INT32.decode(reader))
                    }
                    2 -> {
                        builder.imageOriginal(BYTES.decode(reader))
                    }
                    3 -> {
                        builder.imageRecolored(BYTES.decode(reader))
                    }
                    4 -> {
                        builder.usedBuilderColours.add(RecolorImageColor.ADAPTER.decode(reader))
                    }
                    else -> {
                        val peekFieldEncoding = reader.peekFieldEncoding()
                        builder.addUnknownField(
                            nextTag,
                            peekFieldEncoding!!, peekFieldEncoding.rawProtoAdapter().decode(reader)
                        )
                    }
                }
            }
        }

        // com.squareup.wire.ProtoAdapter
        override fun redact(value: RecolorImage): RecolorImage {
            val newBuilder = value.newBuilder()
            redactElements(newBuilder.usedBuilderColours, RecolorImageColor.ADAPTER)
            newBuilder.clearUnknownFields()
            return newBuilder.build()
        }
    }

    companion object {
        private const val serialVersionUID: Long = 0
        val ADAPTER: ProtoAdapter<RecolorImage> = ProtoAdapterRecolorImage()
    }
}
