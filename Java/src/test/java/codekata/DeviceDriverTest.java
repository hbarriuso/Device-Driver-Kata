package codekata;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.InOrder;

public class DeviceDriverTest {
    private static final int READ_ADDRESS = 0xFF;
    private static final long WRITE_ADDRESS = 0xFFFF;
    private static final long INIT_ADDRESS = 0x0;
    private static final byte PROGRAM_COMMAND = 0x40;
    private static final byte RESET_COMMAND = (byte) 0xFF;
    private static final byte READY_NO_ERROR = 0x00;
    private static final byte READY_BYTE = 0x02;
    private static final byte READY_VPP_ERROR = 0x20;
    private static final byte READY_INTERNAL_ERROR = 0x10;
    private static final byte READY_PROTECTED_BLOCK_ERROR = 0X08;

    private FlashMemoryDevice hardware = mock(FlashMemoryDevice.class);
    private DeviceDriver driver = new DeviceDriver(hardware);

    @Test
    public void readFromHardware() {
        byte expected = 0;
        when(hardware.read(READ_ADDRESS)).thenReturn(expected);
        byte data = driver.read(READ_ADDRESS);
        assertEquals(expected, data);
    }

    @Test
    public void writeToHardwareOk() {
        byte data = 0;
        when(hardware.read(INIT_ADDRESS)).thenReturn(READY_BYTE);
        when(hardware.read(WRITE_ADDRESS)).thenReturn(data);

        driver.write(WRITE_ADDRESS, data);

        verifyWriteFirstSteps(data);
    }

    @Test (expected = RuntimeException.class)
    public void writeToHardwareKoWhenReadingDataFromHardware() {
        byte data = 0;
        when(hardware.read(INIT_ADDRESS)).thenReturn(READY_BYTE);
        byte failedWrittenData = 0x01;
        when(hardware.read(WRITE_ADDRESS)).thenReturn(failedWrittenData);

        driver.write(WRITE_ADDRESS, data);
    }

    @Test
    public void writeToHardwareWithRetries() {
        byte data = 0;
        when(hardware.read(INIT_ADDRESS)).thenReturn(READY_NO_ERROR, READY_BYTE);

        driver.write(WRITE_ADDRESS, data);

        InOrder inOrder = verifyWriteFirstSteps(data);
        inOrder.verify(hardware, times(2)).read(INIT_ADDRESS);
    }

    @Test (expected = RuntimeException.class)
    public void writeToHardwareAvoidInfiniteRetries() {
        byte data = 0;
        when(hardware.read(INIT_ADDRESS)).thenReturn(READY_NO_ERROR);

        driver.write(WRITE_ADDRESS, data);
    }

    @Test (expected = VppException.class)
    public void whenAnErrorBitIsSetWeMustResetTheDevice() {
        byte data = 0;
        when(hardware.read(INIT_ADDRESS)).thenReturn(READY_VPP_ERROR);

        try {
            driver.write(WRITE_ADDRESS, data);
        } finally {
            InOrder inOrder = verifyWriteFirstSteps(data);
            inOrder.verify(hardware).write(INIT_ADDRESS, RESET_COMMAND);
        }
    }

    @Test (expected = InternalErrorException.class)
    public void writeToHardwareCausesAnInternalError() {
        byte data = 0;
        when(hardware.read(INIT_ADDRESS)).thenReturn(READY_INTERNAL_ERROR);

        driver.write(WRITE_ADDRESS, data);
    }

    @Test (expected = ProtectedBlockException.class)
    public void writeToProtectedBlockCausesAnException() {
        byte data = 0;
        when(hardware.read(INIT_ADDRESS)).thenReturn(READY_PROTECTED_BLOCK_ERROR);

        driver.write(WRITE_ADDRESS, data);
    }

    private InOrder verifyWriteFirstSteps(byte data) {
        InOrder inOrder = inOrder(hardware);
        inOrder.verify(hardware).write(INIT_ADDRESS, PROGRAM_COMMAND);
        inOrder.verify(hardware).write(WRITE_ADDRESS, data);
        return inOrder;
    }

}
