package com.haddadian.universalremote.protocol

import com.haddadian.universalremote.atv.proto.RemoteEditInfo
import com.haddadian.universalremote.atv.proto.RemoteImeBatchEdit
import com.haddadian.universalremote.atv.proto.RemoteImeObject
import com.haddadian.universalremote.atv.proto.RemoteMessage
import com.haddadian.universalremote.atv.proto.RemoteSetActive
import com.haddadian.universalremote.atv.proto.RemoteSetVolumeLevel
import com.haddadian.universalremote.atv.proto.RemoteVoicePayload
import com.google.protobuf.ByteString
import com.haddadian.universalremote.atv.proto.PairingEncoding
import com.haddadian.universalremote.atv.proto.PairingMessage
import com.haddadian.universalremote.atv.proto.PairingOption
import com.haddadian.universalremote.atv.proto.EncodingType
import com.haddadian.universalremote.atv.proto.RoleType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Locks critical Android TV v2 wire field numbers against accidental schema drift. */
class AndroidTvRemoteProtocolSchemaTest {
    @Test
    fun volumeUsesFieldSeven() {
        val bytes = RemoteSetVolumeLevel.newBuilder().setVolumeLevel(7).build().toByteArray()
        assertArrayEquals(byteArrayOf(0x38, 0x07), bytes)
    }

    @Test
    fun activeUsesFieldOne() {
        val bytes = RemoteSetActive.newBuilder().setActive(622).build().toByteArray()
        assertArrayEquals(byteArrayOf(0x08, 0xEE.toByte(), 0x04), bytes)
    }

    @Test
    fun imeEditUsesProtocolFieldNumbers() {
        val edit = RemoteEditInfo.newBuilder()
            .setInsert(1)
            .setTextFieldStatus(RemoteImeObject.newBuilder().setStart(0).setEnd(0).setValue("A"))
            .build()
        val bytes = RemoteImeBatchEdit.newBuilder()
            .setImeCounter(0)
            .setFieldCounter(0)
            .addEditInfo(edit)
            .build()
            .toByteArray()
        // Field 3 is the nested edit-info message and field 2 inside it is text_field_status.
        assertTrue(bytes.contains(0x1A.toByte()))
        assertTrue(bytes.contains(0x12.toByte()))
    }

    @Test
    fun voicePayloadUsesSamplesFieldTwo() {
        val bytes = RemoteVoicePayload.newBuilder()
            .setSessionId(1)
            .setSamples(ByteString.copyFrom(byteArrayOf(1, 2)))
            .build()
            .toByteArray()
        assertArrayEquals(byteArrayOf(0x08, 0x01, 0x12, 0x02, 0x01, 0x02), bytes)
    }

    @Test
    fun pairingOptionDeclaresSixDigitHexInputAndInputRole() {
        val option = PairingOption.newBuilder()
            .addInputEncodings(
                PairingEncoding.newBuilder()
                    .setType(EncodingType.ENCODING_TYPE_HEXADECIMAL)
                    .setSymbolLength(6)
            )
            .setPreferredRole(RoleType.ROLE_TYPE_INPUT)
            .build()
        val message = PairingMessage.newBuilder()
            .setProtocolVersion(2)
            .setStatus(PairingMessage.Status.STATUS_OK)
            .setPairingOption(option)
            .build()

        assertTrue(message.hasPairingOption())
        assertTrue(message.pairingOption.inputEncodingsList.any {
            it.type == EncodingType.ENCODING_TYPE_HEXADECIMAL && it.symbolLength == 6
        })
        assertTrue(message.pairingOption.preferredRole == RoleType.ROLE_TYPE_INPUT)
    }

    @Test
    fun remoteMessageWrapsVolumeOnFieldFifty() {
        val inner = RemoteSetVolumeLevel.newBuilder().setVolumeLevel(7).build()
        val bytes = RemoteMessage.newBuilder().setRemoteSetVolumeLevel(inner).build().toByteArray()
        assertArrayEquals(byteArrayOf(0x92.toByte(), 0x03, 0x02, 0x38, 0x07), bytes)
    }
}
