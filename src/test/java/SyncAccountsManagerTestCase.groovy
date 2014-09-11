import com.vbs.SyncAccountsManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import javax.annotation.Resource

/**
 * Created by radams on 9/10/2014.
 */
class SyncAccountsManagerTestCase {

    private SyncAccountsManager manager = new SyncAccountsManager()

    List<Integer> accounts = [10001,10003]

    private File accountsFile

    private String[] args

    @Before
    public void init(){
        initAccountsFile()
        initArgs()
    }

    private void initArgs(){
        args = new String[3]
        args[0] = "${SyncAccountsManager.FILE_NAME_ARGUMENT}=hdap-accounts.txt"
        args[1] = "${SyncAccountsManager.LOGIN_ARGUMENT}=_radams01"
        args[2] = "${SyncAccountsManager.PASSWORD_ARGUMENT}=vocal123"
    }

    private void initAccountsFile(){
        accountsFile = new File("hdap-accounts.txt")
        accountsFile.withWriterAppend { out ->
            accounts.each {account ->
                out.println(account)
            }
        }
    }

    @Test
    public void fileExists(){
        Map<String, Object> parameters = [:]
        parameters.put(SyncAccountsManager.ACCOUNTS_FILE, accountsFile.absolutePath)
        manager.fileExists(SyncAccountsManager.ACCOUNTS_FILE, parameters)
    }

    @Test
    public void getAccounts(){
        Set<Integer> accountsFoundInFile = manager.getAccounts(accountsFile)
        assert accounts.size() == accountsFoundInFile.size()
        assert accountsFoundInFile.containsAll(accounts)
    }

    @Test
    public void initializeFields(){
        Map<String, Object> parameters = [:]
        manager.initializeFields(args, parameters)
        assert args.length == parameters.size()
    }

    @Test
    public void hasValidArguments(){
        Map<String, Object> parameters = [:]
        manager.initializeFields(args, parameters)
        assert manager.hasValidArguments(parameters)
    }

    @Test
    public void doAccountSync(){
        manager.doAccountSync(args, false)
    }
}
