package raytracer.io;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class HdriReader {
    private HdriReader() {
    }

    // Loads a Radiance .hdr file into a linear RGB float buffer (R,G,B interleaved).
    public static HdriImage load(String path) throws IOException {
        try (InputStream input = new BufferedInputStream(new FileInputStream(path))) {
            readHeader(input);
            Resolution resolution = readResolution(input);
            float[] rgb = decodeRleRgbe(input, resolution.width, resolution.height);
            return new HdriImage(resolution.width, resolution.height, rgb);
        }
    }

    // Validates the text header section. This decoder supports RGBE + RLE only.
    private static void readHeader(InputStream input) throws IOException {
        String firstLine = readAsciiLine(input);
        if (firstLine == null || (!firstLine.startsWith("#?RADIANCE") && !firstLine.startsWith("#?RGBE"))) {
            throw new IOException("Unsupported HDR file. Expected #?RADIANCE or #?RGBE header.");
        }

        boolean hasFormat = false;
        while (true) {
            String line = readAsciiLine(input);
            if (line == null) {
                throw new IOException("Unexpected end of file while reading HDR header.");
            }
            if (line.isEmpty()) {
                break;
            }
            if (line.startsWith("FORMAT=")) {
                if (!"FORMAT=32-bit_rle_rgbe".equals(line)) {
                    throw new IOException("Unsupported HDR format: " + line);
                }
                hasFormat = true;
            }
        }

        if (!hasFormat) {
            throw new IOException("HDR header missing FORMAT=32-bit_rle_rgbe.");
        }
    }

    // Reads image size and orientation line. Example: "-Y 1024 +X 2048".
    private static Resolution readResolution(InputStream input) throws IOException {
        String line = readAsciiLine(input);
        if (line == null) {
            throw new IOException("Missing HDR resolution line.");
        }

        String[] tokens = line.trim().split("\\s+");
        if (tokens.length != 4) {
            throw new IOException("Invalid HDR resolution line: " + line);
        }

        String yToken = tokens[0];
        String ySize = tokens[1];
        String xToken = tokens[2];
        String xSize = tokens[3];

        if (!("-Y".equals(yToken) || "+Y".equals(yToken)) || !("-X".equals(xToken) || "+X".equals(xToken))) {
            throw new IOException("Unsupported HDR orientation in resolution line: " + line);
        }

        int height;
        int width;
        try {
            height = Integer.parseInt(ySize);
            width = Integer.parseInt(xSize);
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid HDR resolution numbers: " + line, ex);
        }

        if (width <= 0 || height <= 0) {
            throw new IOException("HDR resolution must be positive: " + line);
        }

        // Keep orientation handling explicit for now. Other valid orientations can be added later.
        if (!"-Y".equals(yToken) || !"+X".equals(xToken)) {
            throw new IOException("Only '-Y +X' HDR orientation is supported right now.");
        }

        return new Resolution(width, height);
    }

    // Decodes new-style Radiance RLE scanlines. Output is row-major RGB float data.
    private static float[] decodeRleRgbe(InputStream input, int width, int height) throws IOException {
        if (width < 8 || width > 32767) {
            throw new IOException("Unsupported HDR scanline width for RLE: " + width);
        }

        // Scratch channel buffers are reused for each row to avoid allocations in the inner loop.
        float[] rgb = new float[width * height * 3];
        byte[] scanlineR = new byte[width];
        byte[] scanlineG = new byte[width];
        byte[] scanlineB = new byte[width];
        byte[] scanlineE = new byte[width];

        for (int y = 0; y < height; y++) {
            //each row starts with 4 bytes
            int b0 = readByte(input);
            int b1 = readByte(input);
            //b2 and b3 are confirmation of the widht
            int b2 = readByte(input);
            int b3 = readByte(input);

            if (b0 != 2 || b1 != 2 || (b2 & 0x80) != 0) {
                throw new IOException("Unsupported non RLE HDR scanline encoding");
            }

            int scanlineWidth = (b2 << 8) | b3;
            if (scanlineWidth != width) {
                throw new IOException("HDR scanline width mismatch. Expected " + width + " but got " + scanlineWidth );
            }

            decodeRleChannel(input, scanlineR);
            decodeRleChannel(input, scanlineG);
            decodeRleChannel(input, scanlineB);
            decodeRleChannel(input, scanlineE);

            int rowOffset = y * width * 3;
            for (int x = 0; x < width; x++) {
                //mask with 11111111 to get lowest 8 bits 0-255 unsigned
                int r = scanlineR[x] & 0xFF;
                int g = scanlineG[x] & 0xFF;
                int b = scanlineB[x] & 0xFF;
                int e = scanlineE[x] & 0xFF;

                int out = rowOffset + x * 3;
                //black pixel
                if (e == 0) {
                    rgb[out] = 0.0f;
                    rgb[out + 1] = 0.0f;
                    rgb[out + 2] = 0.0f;
                } else {
                    // RGBE value = mantissa * 2^(E - 128 - 8), where mantissa is [0..255]
                    float scale = (float) Math.scalb(1.0, e - (128 + 8));
                    rgb[out] = r * scale;
                    rgb[out + 1] = g * scale;
                    rgb[out + 2] = b * scale;
                }
            }
        }

        return rgb;
    }

    // Decodes one RLE compressed channel of a scanline (R, G, B, or E).
    // count > 128: repeated run, count <= 128: literal run.
    private static void decodeRleChannel(InputStream input, byte[] target) throws IOException {
        int x = 0;
        while (x < target.length) {
            int count = readByte(input);
            //repeating values
            if (count > 128) {
                int runLength = count - 128;
                if (runLength <= 0 || x + runLength > target.length) {
                    throw new IOException("Invalid HDR RLE run length");
                }
                byte value = (byte) readByte(input);
                for (int i = 0; i < runLength; i++) {
                    target[x++] = value;
                }
            } else {
                //literal values (not repeated)
                int runLength = count;
                if (runLength <= 0 || x + runLength > target.length) {
                    throw new IOException("Invalid HDR RLE literal length");
                }
                for (int i = 0; i < runLength; i++) {
                    target[x++] = (byte) readByte(input);
                }
            }
        }
    }

    private static int readByte(InputStream input) throws IOException {
        int value = input.read();
        if (value < 0) {
            throw new IOException("Unexpected end of HDR file");
        }
        return value;
    }

    // ASCII line reader so we can parse header text and then continue with binary data.
    private static String readAsciiLine(InputStream input) throws IOException {
        byte[] buffer = new byte[256];
        int size = 0;
        while (true) {
            //read one byte
            int ch = input.read();
            if (ch < 0) {
                //endof file
                return size == 0 ? null : new String(buffer, 0, size, StandardCharsets.US_ASCII);
            }
            if (ch == '\n') {
                break;
            }
            if (ch == '\r') {
                continue;
            }
            if (size == buffer.length) {
                byte[] newBuffer = new byte[buffer.length * 2];
                System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                buffer = newBuffer;
            }
            buffer[size++] = (byte) ch;
        }
        return new String(buffer, 0, size, StandardCharsets.US_ASCII);
    }

    public static final class HdriImage {
        private final int width;
        private final int height;
        //interleaved row major RGB data [r0,g0,b0, r1,g1,b1, ...].
        private final float[] rgb;

        public HdriImage(int width, int height, float[] rgb) {
            this.width = width;
            this.height = height;
            this.rgb = rgb;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public float[] getRgb() {
            return rgb;
        }
    }

    private static final class Resolution {
        private final int width;
        private final int height;

        private Resolution(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}


