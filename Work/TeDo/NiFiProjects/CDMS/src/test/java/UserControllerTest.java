import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.larb.Application;
import org.larb.controllers.UserController;
import org.larb.model.User;
import org.larb.services.UserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ContextConfiguration(classes = Application.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @PostConstruct
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateUser() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password123");

        when(userService.createUser(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/users/register/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\", \"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"));
    }
}