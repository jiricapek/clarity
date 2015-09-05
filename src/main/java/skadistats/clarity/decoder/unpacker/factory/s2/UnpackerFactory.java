package skadistats.clarity.decoder.unpacker.factory.s2;

import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.s2.field.FieldProperties;

public interface UnpackerFactory<T> {

    Unpacker<T> createUnpacker(FieldProperties f);

}
