package xyz.sadiulhakim.notification.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import xyz.sadiulhakim.notification.model.Notification;
import xyz.sadiulhakim.notification.model.NotificationService;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<SseEmitter> subscribe() {
        SseEmitter emitter = notificationService.subscribeCurrentUser();

        return ResponseEntity.ok(emitter);
    }

    @GetMapping("/page")
    String notificationPage(Model model) {
        List<Notification> notifications = notificationService.notificationsOfCurrentUser();
        model.addAttribute("notifications", notifications);

        return "notifications";
    }

    @GetMapping("/delete")
    @ResponseBody
    ResponseEntity<?> delete(@RequestParam long id) {
        notificationService.delete(id);
        return ResponseEntity.ok(Collections.singletonMap("message", "Successfully deleted notification!"));
    }
}
