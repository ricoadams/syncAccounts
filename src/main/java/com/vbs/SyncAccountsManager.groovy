package com.vbs

import org.apache.http.HttpResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource

/**
 * Created by radams on 9/9/2014.
 */
class SyncAccountsManager {

    private static Logger LOGGER = LoggerFactory.getLogger(SyncAccountsManager.class)

    public static final String LOGIN_ARGUMENT = "login"

    public static final String PASSWORD_ARGUMENT = "password"

    public static final String FILE_NAME_ARGUMENT = "filename"

    public static final String ACCOUNTS_FILE = "accountsFile"

    public static final String STOP_PROCESSING_ACCOUNTS_FILE = "stopProcessingAccountsFile"

    public static final String PROCESSED_ACCOUNTS_FILE = "processedAccountsFile"

    public static final String PROCESSED_ACCOUNTS_LOG = "processedAccountsLog"

    public static final File ERROR_LOG = new File("errors.log")

    public static final Integer ARGUMENT_VALUE = 1

    public static final Integer STATUS_OK = 200

    public void doAccountSync(String[] args, boolean deleteFileOnExit = false){
        try {
            Map<String, Object> parameters = initialize(args, deleteFileOnExit)
            Set<Integer> allAccounts = getAccounts(parameters.get(ACCOUNTS_FILE) as File)
            Set<Integer> processedAccounts = getAccounts(parameters.get(PROCESSED_ACCOUNTS_FILE) as File)
            Set<Integer> unprocessedAccounts = getUnprocessedAccounts(allAccounts, processedAccounts)
            processAccounts(unprocessedAccounts, parameters)
        } catch (Throwable ex){
            LOGGER.error("Exception thrown while processing accounts.", ex)
            appendToFile(ERROR_LOG.absolutePath, "Exception thrown while processing accounts. Reson: ${ex}")
        }
    }

    private static void processAccounts(Set<Integer> unprocessedAccounts, Map<String, Object> parameters){
        unprocessedAccounts?.eachWithIndex { int accountId, int index ->
            if (fileExists(STOP_PROCESSING_ACCOUNTS_FILE, parameters)){
                appendToFile(parameters.get(STOP_PROCESSING_ACCOUNTS_FILE), "Processing stopped.")
                LOGGER.info("Stopping account processing...")
                return
            } else {
                LOGGER.info("Processing account ${index + 1} of ${unprocessedAccounts.size()} accounts... account id: ${accountId}")
                processAccount(accountId, parameters)
            }
        }
    }

    private static void processAccount(int accountId, Map<String, Object> parameters) {
        HttpResponse response = SyncAccountRESTProxy.callService(parameters.get(LOGIN_ARGUMENT), parameters.get(PASSWORD_ARGUMENT), accountId)
        if (successful(response)) {
            appendToFile(parameters.get(PROCESSED_ACCOUNTS_FILE), accountId as String, false)
            appendToFile(parameters.get(PROCESSED_ACCOUNTS_LOG), "Processed account id: ${accountId}")
        } else {
            appendToFile(parameters.get(PROCESSED_ACCOUNTS_LOG), "FAIL! account id: ${accountId}")
            appendToFile(ERROR_LOG.absolutePath, "FAIL! account id: ${accountId}. Reason: ${response.statusLine.reasonPhrase}")
        }
    }

    private static Map<String, Object> initialize(String[] args, boolean deleteFileOnExit = false){
        Map<String, Object> parameters = [:]
        initializeFields(args, parameters)
        initializeFiles(parameters, deleteFileOnExit)
        validate(parameters)
        return parameters
    }

    private static void initializeFields(String[] args, Map<String, Object> parameters) {
        args?.each { arg ->
            if (arg.toLowerCase().startsWith(LOGIN_ARGUMENT)) {
                parameters.put(LOGIN_ARGUMENT, getValue(arg))
            } else if (arg.toLowerCase().startsWith(PASSWORD_ARGUMENT)) {
                parameters.put(PASSWORD_ARGUMENT, getValue(arg))
            } else if (arg.toLowerCase().startsWith(FILE_NAME_ARGUMENT)) {
                parameters.put(FILE_NAME_ARGUMENT, getValue(arg))
            }
        }
    }

    private static void initializeFiles(Map<String, Object> parameters, boolean deleteFileOnExit = false){
        if (parameters.containsKey(FILE_NAME_ARGUMENT)){
            parameters.put(ACCOUNTS_FILE, new File(parameters.get(FILE_NAME_ARGUMENT) as String).absolutePath)

            File processedAccountsFile = new File("${parameters.get(FILE_NAME_ARGUMENT)}.processed")
            processedAccountsFile.createNewFile()
            parameters.put(PROCESSED_ACCOUNTS_FILE, processedAccountsFile.absolutePath)

            File processedAccountsLog = new File("${parameters.get(FILE_NAME_ARGUMENT)}.log")
            processedAccountsLog.createNewFile()
            parameters.put(PROCESSED_ACCOUNTS_LOG, processedAccountsLog.absolutePath)

            parameters.put(STOP_PROCESSING_ACCOUNTS_FILE, new File("${parameters.get(FILE_NAME_ARGUMENT)}.stop").absolutePath)

            if (deleteFileOnExit){
                (parameters.get(ACCOUNTS_FILE) as File).deleteOnExit()
                (parameters.get(PROCESSED_ACCOUNTS_FILE) as File).deleteOnExit()
                (parameters.get(PROCESSED_ACCOUNTS_LOG) as File).deleteOnExit()
                ERROR_LOG.deleteOnExit()
            }
        }
    }

    private static void validate(Map<String, Object> parameters){
        if (hasValidArguments(parameters)){
            if (!fileExists(ACCOUNTS_FILE, parameters)){
                throw new Throwable("File not found: ${parameters.get(ACCOUNTS_FILE)}")
            }
        } else {
            throw new Throwable("Please in the following parameters: ${LOGIN_ARGUMENT}, ${PASSWORD_ARGUMENT}, ${FILE_NAME_ARGUMENT}")
        }
    }

    private static boolean hasValidArguments(Map<String, Object> parameters) {
        return parameters.containsKey(LOGIN_ARGUMENT) && parameters.containsKey(PASSWORD_ARGUMENT) && parameters.containsKey(FILE_NAME_ARGUMENT)
    }

    private static boolean fileExists(String fileName, Map<String, Object> parameters) {
        boolean exists = false
        if (parameters.containsKey(fileName)){
            if (new File(parameters.get(fileName)).exists()){
                exists = true
            }
        }

        return exists
    }

    private static Set<Integer> getAccounts(File accountsFile){
        return accountsFile.collect { it as Integer } as TreeSet
    }

    private static Set<Integer> getUnprocessedAccounts(Set<Integer> allAccounts, Set<Integer> processedAccounts) {
        return allAccounts?.findAll { accountId ->
            !processedAccounts.contains(accountId)
        }
    }

    private static Boolean successful(HttpResponse response) {
        return (STATUS_OK == response.statusLine.statusCode)
    }

    private static appendToFile(String fileName, String line, Boolean includeDate = true) {
        if (includeDate){
            new File(fileName).withWriterAppend { out -> out.println("${new Date()}: ${line}")}
        } else {
            new File(fileName).withWriterAppend { out -> out.println(line)}
        }
    }

    private static String getValue(String argument) {
        return argument.split("=")[ARGUMENT_VALUE]
    }

    public static void main(final String[] args) {
        try {
            LOGGER.info("Processing started at ${new Date()}...")
            new SyncAccountsManager().doAccountSync(args)
        } catch (Throwable ex) {
            LOGGER.error("Exception thrown while processing accounts.", ex)
        } finally {
            LOGGER.info("Processing ended at ${new Date()}...")
        }
    }
}
