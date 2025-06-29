package xyz.sadiulhakim.group;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xyz.sadiulhakim.group.service.ChatGroupService;

import java.util.UUID;

@Controller
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupController {

    private final ChatGroupService groupService;

    @GetMapping("/create")
    String createGroup(@RequestParam String name) {
        groupService.create(name);
        return "redirect:/chat";
    }

    @GetMapping("/remove/{groupId}/{memberId}")
    String removeMember(@PathVariable String groupId, @PathVariable String memberId, RedirectAttributes model) {
        UUID group = UUID.fromString(groupId);
        UUID member = UUID.fromString(memberId);
        String message = groupService.removeFromGroup(group, member);
        model.addFlashAttribute("isGroupAction", true);
        model.addFlashAttribute("groupActionMessage", message);
        return "redirect:/chat/" + groupId + "/" + "GROUP";
    }

    @GetMapping("/add/{groupId}/{memberId}")
    String addMember(@PathVariable String groupId, @PathVariable String memberId, RedirectAttributes model) {
        UUID group = UUID.fromString(groupId);
        UUID member = UUID.fromString(memberId);
        String message = groupService.addToGroup(group, member);
        model.addFlashAttribute("isGroupAction", true);
        model.addFlashAttribute("groupActionMessage", message);
        return "redirect:/chat/" + groupId + "/" + "GROUP";
    }

    @GetMapping("/close/{groupId}")
    String closeGroup(@PathVariable String groupId, RedirectAttributes model) {
        UUID group = UUID.fromString(groupId);
        String message = groupService.closeGroup(group);
        model.addFlashAttribute("isGroupAction", true);
        model.addFlashAttribute("groupActionMessage", message);
        return "redirect:/chat";
    }

    @GetMapping("/leave/{groupId}")
    String leaveGroup(@PathVariable String groupId, RedirectAttributes model) {
        UUID group = UUID.fromString(groupId);
        String message = groupService.leaveGroup(group);
        model.addFlashAttribute("isGroupAction", true);
        model.addFlashAttribute("groupActionMessage", message);
        return "redirect:/chat";
    }
}
