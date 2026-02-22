package org.ctrl.extras;

/**
 * Simple tag model for SCADA/PLC communication.
 */
public class Tag {

    public enum Area {
        DM,
        RR
    }

    public enum DataType {
        WORD(1),
        DWORD(2),
        BIT(0);

        private final int wordLength;

        DataType(int wordLength) {
            this.wordLength = wordLength;
        }

        public int getWordLength() {
            return wordLength;
        }
    }

    private final String name;
    private final Area area;
    private final int address;
    private final DataType dataType;
    private final Integer bit;

    public Tag(String name, Area area, int address, DataType dataType) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name is required");
        }
        if (area == null) {
            throw new IllegalArgumentException("Area is required");
        }
        if (dataType == null) {
            throw new IllegalArgumentException("Data type is required");
        }
        if (address < 0) {
            throw new IllegalArgumentException("Address must be >= 0");
        }
        if (dataType == DataType.BIT) {
            throw new IllegalArgumentException("BIT tags must provide a bit index");
        }
        this.name = name;
        this.area = area;
        this.address = address;
        this.dataType = dataType;
        this.bit = null;
    }

    public Tag(String name, Area area, int address, int bit) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name is required");
        }
        if (area == null) {
            throw new IllegalArgumentException("Area is required");
        }
        if (address < 0) {
            throw new IllegalArgumentException("Address must be >= 0");
        }
        if (bit < 0 || bit > 15) {
            throw new IllegalArgumentException("Bit must be between 0 and 15");
        }
        this.name = name;
        this.area = area;
        this.address = address;
        this.dataType = DataType.BIT;
        this.bit = bit;
    }

    public static Tag dmWord(String name, int address) {
        return new Tag(name, Area.DM, address, DataType.WORD);
    }

    public static Tag dmDWord(String name, int address) {
        return new Tag(name, Area.DM, address, DataType.DWORD);
    }

    public static Tag rrBit(String name, int address, int bit) {
        return new Tag(name, Area.RR, address, bit);
    }

    public String getName() {
        return name;
    }

    public Area getArea() {
        return area;
    }

    public int getAddress() {
        return address;
    }

    public DataType getDataType() {
        return dataType;
    }

    public int getLengthWords() {
        if (isBit()) {
            throw new IllegalStateException("BIT tag does not have word length");
        }
        return dataType.getWordLength();
    }

    public Integer getBit() {
        return bit;
    }

    public boolean isBit() {
        return dataType == DataType.BIT;
    }

    public String getAddressBit() {
        if (!isBit()) {
            throw new IllegalStateException("Tag is not BIT");
        }
        return String.format("%d.%02d", address, bit);
    }

    public MemoryVariable toMemoryVariable() {
        if (isBit()) {
            throw new IllegalStateException("BIT tag does not map to MemoryVariable");
        }
        return new MemoryVariable(name, area.name(), address, getLengthWords());
    }

    public static final Tag DM_0000_WORD = dmWord("DM_0000", 0);
    public static final Tag DM_0000_DWORD = dmDWord("DM_0000", 0);
    public static final Tag PECAPH29 = dmDWord("PECAPH29", 100);
    public static final Tag PECAPH30 = dmDWord("PECAPH30", 102);
    public static final Tag PECAPH31 = dmDWord("PECAPH31", 104);
    public static final Tag PECAROLLERCARGA41 = dmDWord("PECAROLLERCARGA41", 100);
    public static final Tag PECAROLLERDESC41 = dmDWord("PECAROLLERDESC41", 102);
    public static final Tag PECAROLLERCARGA42 = dmDWord("PECAROLLERCARGA42", 104);
    public static final Tag PECAROLLERDESC42 = dmDWord("PECAROLLERDESC42", 12);
    public static final Tag PECAROLLERCARGA43 = dmDWord("PECAROLLERCARGA43", 110);
    public static final Tag PECAROLLERDESC43 = dmDWord("PECAROLLERDESC43", 16);
    public static final Tag QUALIDADE41 = dmDWord("QUALIDADE41", 18);
    public static final Tag QUALIDADE42 = dmDWord("QUALIDADE42", 20);
    public static final Tag QUALIDADE43 = dmDWord("QUALIDADE43", 22);
}
