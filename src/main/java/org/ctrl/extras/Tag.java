package org.ctrl.extras;

/**
 * Simple tag model for SCADA/PLC communication.
 */
public class Tag {

    public enum Area {
        DM
    }

    public enum DataType {
        WORD(1),
        DWORD(2);

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
        this.name = name;
        this.area = area;
        this.address = address;
        this.dataType = dataType;
    }

    public static Tag dmWord(String name, int address) {
        return new Tag(name, Area.DM, address, DataType.WORD);
    }

    public static Tag dmDWord(String name, int address) {
        return new Tag(name, Area.DM, address, DataType.DWORD);
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
        return dataType.getWordLength();
    }

    public MemoryVariable toMemoryVariable() {
        return new MemoryVariable(name, area.name(), address, getLengthWords());
    }

    public static final Tag DM_0000_WORD = dmWord("DM_0000", 0);
    public static final Tag DM_0000_DWORD = dmDWord("DM_0000", 0);
}
