package test.demo;

import org.ctrl.extras.Tag;
import org.ctrl.extras.MemoryVariable;

/**
 * Simple functional test for Tag DM.
 */
public class TestTagDM {

    public static void main(String[] args) {
        Tag wordTag = Tag.dmWord("DM_0000", 0);
        Tag dwordTag = Tag.dmDWord("DM_0000", 0);

        printTag("WORD", wordTag);
        printTag("DWORD", dwordTag);
    }

    private static void printTag(String label, Tag tag) {
        MemoryVariable mv = tag.toMemoryVariable();
        System.out.println(label + " TAG");
        System.out.println("name=" + tag.getName());
        System.out.println("area=" + tag.getArea());
        System.out.println("address=" + tag.getAddress());
        System.out.println("lengthWords=" + tag.getLengthWords());
        System.out.println("memoryVariable=" + mv.toString());
        System.out.println();
    }
}
