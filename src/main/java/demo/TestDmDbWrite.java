package demo;

import org.ctrl.db.config.DbConfig;
import org.ctrl.db.service.DmValueService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TestDmDbWrite {

    public static void main(String[] args) {
        int address = args.length > 0 ? Integer.parseInt(args[0]) : 0;
        int value = args.length > 1 ? Integer.parseInt(args[1]) : 1334;

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(DbConfig.class);
        try {
            DmValueService service = ctx.getBean(DmValueService.class);
            service.saveValue(address, value);
            System.out.println("Saved DM address=" + address + " value=" + value);
            System.out.println("DB value=" + service.getByAddress(address).orElse(null));
        } finally {
            ctx.close();
        }
    }
}
