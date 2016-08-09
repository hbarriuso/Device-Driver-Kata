package codekata;

/**
 * This class is used by the operating system to interact with the hardware 'FlashMemoryDevice'.
 */
public class DeviceDriver {

    private static final long INIT_ADDRESS = 0X0;
    private static final byte PROGRAM_COMMAND = 0X40;
    private static final byte READY_MASK = 0x02;
    private static final byte READY_NO_ERROR = 0x00;
    private static final long TIMEOUT_THRESHOLD = 100_000_000;
    private static final byte RESET_COMMAND = (byte) 0xFF;
    private static final byte VPP_MASK = 0x20;
    private static final byte INTERNAL_ERROR_MASK = 0x10;
    private static final byte PROTECTED_BLOCK_ERROR_MASK = 0X08;

    private final FlashMemoryDevice hardware;

    public DeviceDriver(FlashMemoryDevice hardware) {
        this.hardware = hardware;
    }

    public byte read(long address) {
        return hardware.read(address);
    }

    public void write(long address, byte data) {
        long start = System.nanoTime();
        hardware.write(INIT_ADDRESS, PROGRAM_COMMAND);
        hardware.write(address, data);
        byte readyByte;
        while(((readyByte = read(INIT_ADDRESS)) & READY_MASK) == 0) {
           if (readyByte != READY_NO_ERROR) {
               hardware.write(INIT_ADDRESS, RESET_COMMAND);
               if ((readyByte & VPP_MASK) > 0) {
                   throw new VppException();
               }
               if ((readyByte & INTERNAL_ERROR_MASK) > 0) {
                   throw new InternalErrorException();
               }
               if ((readyByte & PROTECTED_BLOCK_ERROR_MASK) > 0) {
                   throw new ProtectedBlockException();
               }
           }
           if (System.nanoTime() - start > TIMEOUT_THRESHOLD) {
               throw new RuntimeException("Timeout when trying to read data from memory");
           }
        }
        byte actual = read(address);
        if (data != actual) {
           throw new RuntimeException("Failed to read data from memory");
        }
    }
}
