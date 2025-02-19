package xyz.sadiulhakim.user.model;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.*;
import org.springframework.modulith.NamedInterface;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import xyz.sadiulhakim.util.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@NamedInterface("user-service")
@RequiredArgsConstructor
public class UserService {

    private final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepo;
    private final AppProperties appProperties;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final ConnectionRequestRepo connectionRequestRepo;

    @Async("taskExecutor")
    @EventListener
    void appStarted(ApplicationReadyEvent event) {
        User user = userRepo.findByEmail("sadiulhakim@gmail.com").orElse(null);
        if (user == null) {
            userRepo.save(
                    new User(null, "Sadiul", "Hakim", "sadiulhakim@gmail.com",
                            passwordEncoder.encode("hakim@123"), "ROLE_ADMIN",
                            appProperties.getDefaultUserPhotoName(), ColorUtil.getRandomColor(),
                            new ArrayList<>(), LocalDateTime.now())
            );
        }
    }

    public User findByEmail(String email) {

        return userRepo.findByEmail(email).orElse(null);
    }

    public Optional<User> findById(UUID userId) {

        Optional<User> user = userRepo.findById(userId);
        if (user.isEmpty()) {
            LOGGER.error("UserService.getById :: Could not find user {}", userId);
        }

        return user;
    }

    public String connect(UUID toUser) {

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            return "User is not authenticated";
        }

        User user = findByEmail(authentication.getName());
        if (user == null) {
            LOGGER.warn("User does not exists with email : {}", authentication.getName());
            return "User does not exists with email : " + authentication.getName();
        }

        Optional<User> toUserById = findById(toUser);
        if (toUserById.isEmpty()) {
            LOGGER.warn("User does not exists with id : {}", toUser);
            return "User does not exists with id : " + toUser;
        }

        if (user.getConnectedUsers().contains(toUserById.get().getId())) {
            return "You are already connected with : " + user.getLastname();
        }

        if (alreadySentRequest(user.getId(), toUser)) {
            return "Connection request is already sent to user : " + toUser;
        }

        ConnectionRequest connectionRequest = new ConnectionRequest(null, user, toUserById.get(),
                LocalDateTime.now());
        connectionRequestRepo.save(connectionRequest);

        return "Successfully sent connection Request to  " + toUserById.get().getLastname();
    }

    public boolean alreadySentRequest(UUID userId, UUID toUser) {

        User user = findById(userId).orElse(null);
        User toUserObj = findById(toUser).orElse(null);
        if (user == null || toUserObj == null) {
            return false;
        }

        return connectionRequestRepo.findByUserAndToUser(user, toUserObj).isPresent();
    }

    public String cancelConnection(long requestId) {

        Optional<ConnectionRequest> req = connectionRequestRepo.findById(requestId);
        if (req.isEmpty()) {
            return "Connection Request with id " + requestId + " does not exist";
        }

        ConnectionRequest connectionRequest = req.get();
        connectionRequestRepo.delete(connectionRequest);

        return "Successfully cancelled connection Request  id: " + requestId;
    }

    public String acceptConnection(long reqId) {

        Optional<ConnectionRequest> req = connectionRequestRepo.findById(reqId);
        if (req.isEmpty()) {
            return "Connection Request with id " + reqId + " does not exist";
        }

        ConnectionRequest connectionRequest = req.get();
        User user = connectionRequest.getUser();
        User toUser = connectionRequest.getToUser();

        user.getConnectedUsers().add(toUser.getId());
        toUser.getConnectedUsers().add(user.getId());

        userRepo.saveAll(List.of(user, toUser));

        connectionRequestRepo.delete(connectionRequest);
        return "You accepted Connection Request from " + toUser.getLastname();
    }

    public String removeConnection(UUID toUser) {

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            return "User id not authenticated!";
        }

        User user = findByEmail(authentication.getName());
        Optional<User> byIdUser = findById(toUser);
        if (byIdUser.isEmpty()) {
            return "Could not find user with id : " + toUser;
        }

        User toUserObj = byIdUser.get();
        user.getConnectedUsers().remove(toUserObj.getId());
        toUserObj.getConnectedUsers().remove(user.getId());

        userRepo.saveAll(List.of(user, toUserObj));

        return "You successfully removed " + toUserObj.getLastname() + " from you connection!";
    }

    public List<ConnectionRequest> sentConnectionRequests() {

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            return Collections.emptyList();
        }

        User user = findByEmail(authentication.getName());
        return connectionRequestRepo.findAllByUser(user);
    }

    public PaginationResult connections() {

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            return new PaginationResult();
        }

        User user = findByEmail(authentication.getName());
        if (user == null) {
            return new PaginationResult();
        }

        Page<User> page = userRepo.findAllByIdIn(user.getConnectedUsers(), PageRequest.of(0, appProperties.getPaginationSize()));
        return PageUtil.prepareResult(page);
    }

    public List<ConnectionRequest> receivedConnectionRequests() {

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            return Collections.emptyList();
        }

        User user = findByEmail(authentication.getName());
        return connectionRequestRepo.findAllByToUser(user);
    }

    public void save(UserDTO user) {

        try {

            LOGGER.info("UserService.save :: saving/updating user {}", user.getEmail());

            User existingUser = findByEmail(user.getEmail());
            if (existingUser != null) {
                LOGGER.warn("UserService.save :: User with email {} already exists!", user.getEmail());
            }

            User userModel = modelMapper.map(user, User.class);
            userModel.setPicture(appProperties.getDefaultUserPhotoName());
            userModel.setRole("ROLE_USER");
            userModel.setCreatedAt(LocalDateTime.now());
            userModel.setPassword(passwordEncoder.encode(user.getRawPassword()));
            userModel.setTextColor(ColorUtil.getRandomColor());
            userRepo.save(userModel);

        } catch (Exception ex) {
            LOGGER.error("UserService.save :: {}", ex.getMessage());
        }
    }

    private void update(UUID userId, UserDTO user, MultipartFile photo) throws IOException {

        User exUser = findById(userId).orElse(null);
        if (exUser == null) {
            LOGGER.warn("UserService.update :: User does not exists with id {}", user);
            return;
        }

        if (StringUtils.hasText(user.getFirstname())) {
            exUser.setFirstname(user.getFirstname());
        }

        if (StringUtils.hasText(user.getLastname())) {
            exUser.setLastname(user.getLastname());
        }

        if (photo != null && !Objects.requireNonNull(photo.getOriginalFilename()).isEmpty()) {
            String fileName = FileUtil.uploadFile(appProperties.getUserImageFolder(), photo.getOriginalFilename(), photo.getInputStream());

            if (StringUtils.hasText(fileName) && !exUser.getPicture().equals(appProperties.getDefaultUserPhotoName())) {
                boolean deleted = FileUtil.deleteFile(appProperties.getUserImageFolder(), exUser.getPicture());
                if (deleted) {
                    LOGGER.info("UserService.update :: File {} is deleted", exUser.getPicture());
                }
            }

            if (StringUtils.hasText(fileName)) {
                exUser.setPicture(fileName);
            }
        }

        userRepo.save(exUser);
    }

    public PaginationResult findAllPaginated(int pageNumber) {
        return findAllPaginatedWithSize(pageNumber, appProperties.getPaginationSize());
    }

    public PaginationResult findAllPaginatedWithSize(int pageNumber, int size) {

        LOGGER.info("UserService.findAllPaginated :: finding user page : {}", pageNumber);
        Page<User> page = userRepo.findAll(PageRequest.of(pageNumber, size, Sort.by("name")));
        return PageUtil.prepareResult(page);
    }

    public PaginationResult searchUser(String text, int pageNumber, boolean filterByConnection) {

        LOGGER.info("UserService.searchUser :: search user by text : {}", text);
        Page<User> page = userRepo.findByFirstnameContainingOrLastnameContainingOrEmailContaining(text, text, text,
                PageRequest.of(pageNumber, 200));

        if (filterByConnection) {
            List<User> users = filterUserByConnection(page);
            return PageUtil.prepareResult(page, users);
        }

        return PageUtil.prepareResult(page);
    }

    private List<User> filterUserByConnection(Page<User> page) {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        User user = findByEmail(authentication.getName());
        if (user == null) {
            return Collections.emptyList();
        }

        List<UUID> connections = user.getConnectedUsers();

        if (connections.isEmpty()) {
            return page.getContent(); // No connections, nothing to filter
        }

        // Filter users who are NOT in the connections list
        return page.getContent()
                .stream()
                .filter(u -> !connections.contains(u.getId()))
                .toList(); // Create a new filtered list
    }

    public void delete(UUID id) {

        Optional<User> user = userRepo.findById(id);
        user.ifPresent(u -> {

            if (!u.getPicture().equals(appProperties.getDefaultUserPhotoName())) {
                boolean deleted = FileUtil.deleteFile(appProperties.getUserImageFolder(), u.getPicture());
                if (deleted) {
                    LOGGER.info("UserService.delete :: deleted file {}", u.getPicture());
                }
            }
            userRepo.delete(u);
        });
    }

    public String changePassword(PasswordDTO passwordDTO, UUID userId) {

        try {
            Optional<User> user = findById(userId);
            if (user.isEmpty()) {
                return "User does not exist!";
            }

            if (!StringUtils.hasText(passwordDTO.getCurrentPassword()) ||
                    !StringUtils.hasText(passwordDTO.getNewPassword()) ||
                    !StringUtils.hasText(passwordDTO.getConfirmPassword())) {
                return "Password can not be empty!";
            }

            if (!passwordDTO.getNewPassword().equals(passwordDTO.getConfirmPassword())) {
                return "Password Does not Match!";
            }

            User exUser = user.get();
            if (passwordEncoder.matches(passwordDTO.getNewPassword(), exUser.getPassword())) {
                return "Invalid Password!";
            }

            exUser.setPassword(passwordEncoder.encode(passwordDTO.getNewPassword()));
            userRepo.save(exUser);

            return "Password is reset successfully!";
        } catch (Exception ex) {
            LOGGER.error("UserService.changePassword :: {}", ex.getMessage());
            return "Could not reset password, Please try again!";
        }
    }

    public long count() {
        return userRepo.numberOfUsers();
    }

    public byte[] getCsvData() {
        final int batchSize = 500;
        int batchNumber = 0;
        StringBuilder sb = new StringBuilder("Id,First Name,Last Name,Email,Role,Picture,Text Color,Date\n");
        Page<User> page;
        do {
            page = userRepo.findAll(PageRequest.of(batchNumber, batchSize));
            List<User> users = page.getContent();
            for (User user : users) {
                sb.append(user.getId())
                        .append(",")
                        .append(user.getFirstname())
                        .append(",")
                        .append(user.getLastname())
                        .append(",")
                        .append(user.getEmail())
                        .append(",")
                        .append(user.getRole())
                        .append(",")
                        .append(user.getPicture())
                        .append(",")
                        .append(user.getTextColor())
                        .append(",")
                        .append(DateUtil.format(user.getCreatedAt()))
                        .append("\n");

            }
            batchNumber++;
        } while (page.hasNext());

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
