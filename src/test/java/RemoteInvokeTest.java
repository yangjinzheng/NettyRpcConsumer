import com.netty.annotion.RemoteInvoke;
import com.netty.param.Response;
import com.netty.user.bean.User;
import com.netty.user.remote.UserRemote;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RemoteInvokeTest.class)
@ComponentScan("com.netty")
public class RemoteInvokeTest {
    @RemoteInvoke
    private UserRemote userRemote;
    @Test
    public void test(){
        User u = new User();
        u.setId(1);
        u.setName("张三");
        Response response = userRemote.saveUser(u);
        System.out.println(response);
    }

}
