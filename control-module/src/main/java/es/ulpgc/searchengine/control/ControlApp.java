package es.ulpgc.searchengine.control;

import io.javalin.Javalin;

public class  ControlApp {
    public static void main(String[] args) {
        Controller controller = new Controller();
        Javalin app = Javalin.create(cfg -> cfg.http.defaultContentType = "application/json").start(7000);
        controller.register(app);
        System.out.println("Control Module running on port 7000");
    }
}
