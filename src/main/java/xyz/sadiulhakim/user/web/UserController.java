package xyz.sadiulhakim.user.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.sadiulhakim.user.model.ConnectionRequest;
import xyz.sadiulhakim.user.model.User;
import xyz.sadiulhakim.user.model.UserService;
import xyz.sadiulhakim.util.PaginationResult;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
class UserController {

    private final UserService userService;

    @GetMapping("/search")
    String searchUser(@RequestParam String text, Model model) {
        PaginationResult paginationResult = userService.searchUser(text, 0, true);
        model.addAttribute("result", paginationResult);
        return "display_users";
    }

    @PostMapping("/change_picture")
    String changeProfilePicture(@RequestParam MultipartFile photo, @RequestParam UUID userId) {
        userService.updateProfilePicture(photo, userId);
        return "redirect:/user/profile";
    }

    @PostMapping("/update_profile")
    String updateProfile(@ModelAttribute User user) {
        userService.update(user);
        return "redirect:/user/profile";
    }

    @GetMapping("/send-connect-request")
    @ResponseBody
    ResponseEntity<?> connect(@RequestParam UUID toUser) {
        String connect = userService.connect(toUser);
        return ResponseEntity.ok(Collections.singletonMap("message", connect));
    }

    @GetMapping("/remove-connection")
    @ResponseBody
    ResponseEntity<?> removeConnection(@RequestParam UUID toUser) {
        String connect = userService.removeConnection(toUser);
        return ResponseEntity.ok(Collections.singletonMap("message", connect));
    }

    @GetMapping("/cancel-connect-request")
    @ResponseBody
    ResponseEntity<?> cancelConnection(@RequestParam long requestId) {
        String connect = userService.cancelConnection(requestId);
        return ResponseEntity.ok(Collections.singletonMap("message", connect));
    }

    @GetMapping("/accept-connect-request")
    @ResponseBody
    ResponseEntity<?> acceptConnection(@RequestParam long requestId) {
        String connect = userService.acceptConnection(requestId);
        return ResponseEntity.ok(Collections.singletonMap("message", connect));
    }

    @GetMapping("/sent_connections_page")
    String sentConnectionsPage(Model model) {

        List<ConnectionRequest> requests = userService.getAllSentConnectionRequests();
        model.addAttribute("requests", requests);
        return "sentRequests";
    }

    @GetMapping("/received_connections_page")
    String receivedConnectionPage(Model model) {

        List<ConnectionRequest> requests = userService.receivedConnectionRequests();
        model.addAttribute("requests", requests);
        return "receivedRequests";
    }

    @GetMapping("/connections_page")
    String connectionsPage(Model model) {

        var connections = userService.connections();
        model.addAttribute("result", connections);
        return "connections";
    }

    @GetMapping("/profile")
    String profile_page(Model model) {

        Optional<User> user = userService.currentUser();
        model.addAttribute("user", user.orElseGet(User::new));
        return "profile";
    }
}
