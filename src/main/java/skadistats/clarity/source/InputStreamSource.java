package skadistats.clarity.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.exception.BytesNotReadException;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamSource extends Source {

    private static final Logger log = LoggerFactory.getLogger(InputStreamSource.class);

    private InputStream stream;
    private String path;
    private int position;
    private byte[] dummy = new byte[32768];

    private int lastOffset = -1;

    public InputStreamSource(InputStream stream, String path) {
        this.stream = stream;
        this.path = path;
        this.position = 0;
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public void setPosition(int newPosition) throws IOException {
        if (position > newPosition) {
            throw new UnsupportedOperationException("cannot rewind input stream");
        }
        while (position != newPosition) {
            int r = Math.min(dummy.length, newPosition - position);
            try {
                readBytes(dummy, 0, r);
            }catch (BytesNotReadException ex){
                log.debug("Reached end of file. Save offsed counter and skip processing.");
            }
        }
    }

    @Override
    public byte readByte() throws IOException, BytesNotReadException {
        int i = stream.read();
        if (i == -1) {
            // close old one and open new one (with more data)
            stream.close();
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
            InputStreamSource src = new InputStreamSource(bis, path);
            stream = src.stream;

            throw new BytesNotReadException();
        }
        position++;
        return (byte) i;
    }

    @Override
    public void readBytes(byte[] dest, int offset, int length) throws IOException, BytesNotReadException {
        if(lastOffset != -1){
            offset = lastOffset;
        }

        while (length > 0) {
            int r = stream.read(dest, offset, length);
            if (r == -1) {
                //throw new EOFException();
                // when on end of file does not throw exception but skip it and remember where it stopped
                lastOffset = offset;
                throw new BytesNotReadException();
            }else{
                lastOffset = -1;
            }
            position += r;
            offset += r;
            length -= r;
        }
    }

}
