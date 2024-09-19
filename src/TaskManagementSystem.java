import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;

// Enum for task status
enum TaskStatus {
    TODO, COMPLETED
}

// Enum for task priority 
enum TaskPriority{
    HIGH, MEDIUM, LOW;
}

// Methods used for Personal task management system.
class Task {
    Scanner scanner = new Scanner(System.in);
    final private String description;
    final private Date deadline;
    private TaskStatus status;
    final private Date assigning_Date;
    final private int task_id;
    private TaskPriority priority;

    public Task(int task_id, String task_description, Date assigning_Date, Date deadline_Date, TaskPriority priority) {
        this.description = task_description;
        this.deadline = deadline_Date;
        this.assigning_Date = assigning_Date;
        this.task_id = task_id;
        this.status = TaskStatus.TODO;
        this.priority = priority;
    }

    public int getTask_id() {
        return task_id;
    }

    public String getDescription() {
        return description;
    }

    public Date getDeadline() {
        return deadline;
    }

    public Date getAssigningDate() {
        return assigning_Date;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void markCompleted(){
        this.status = TaskStatus.COMPLETED;
    }

    public TaskPriority getPriority(){
        return priority;
    }
    public void TaskPriority(String task_description)throws Exception{
        System.out.println("Enter the value of priority of the task(HIGH,MEDIUM,LOW)");
        String priorityStr = scanner.nextLine();
        Connection con = DatabaseConnectionManager.getConnection();
        if (con!= null) {
            String sql = "Update personal_task SET TaskPriority = ? WHERE task_description = ? ";
            PreparedStatement pst = con.prepareCall(sql);
            pst.setString(1,priorityStr);
            pst.setString(2,task_description);
            pst.executeUpdate();
            con.close();
        }
        TaskPriority priority_local = TaskPriority.valueOf(priorityStr);
        this.priority = priority_local;
    }
    

    // Removing the task 
    public void removeCompletely(String task_description) {
        try {
            Connection con = DatabaseConnectionManager.getConnection();
            String sql = "DELETE FROM personal_task WHERE task_description = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, task_description);
            int rowsEffected = pst.executeUpdate();
            if (rowsEffected > 0) {
                System.out.println("The task has been deleted.");
            } else {
                System.out.println("No rows affected. Something went wrong.");
            }
        } catch (Exception e) {
            System.out.println("Something went wrong. Please verify and try again.");
        }
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return "Task id: " + task_id + " ,Task description: " + description + ", Deadline: " + dateFormat.format(deadline) + ", Status: " + status + " Task Priority: " + priority;
    }
}

// Methods used for organisation task management 
class Organization {
    final private String name;
    final private Map<String, String> roles; // username -> role (team leader, executive head, member)
    final private Map<String, List<Task>> tasks; // username -> list of tasks assignee
    final private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    public Organization(String name)throws Exception {
        this.name = name;
        this.roles = new HashMap<>();
        this.tasks = new HashMap<>();
        
        
            fetchOrganizationData();
            fetchOrganizationTasks();
        
    }

     
    Scanner scanner = new Scanner(System.in);
    // Method to fetch the data from the organisation table.
    private void fetchOrganizationData() throws Exception {
        Connection con = DatabaseConnectionManager.getConnection();
        String sql = "SELECT * FROM organization WHERE organization_name = ?";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setString(1, this.name);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            String username = rs.getString("username");
            String role = rs.getString("role");
            roles.put(username, role);
            tasks.put(username, new ArrayList<>());
        }
    }

    // Method to fetch the data from the organisation_task table.
    private void fetchOrganizationTasks() throws Exception {
        Connection con = DatabaseConnectionManager.getConnection();
        String sql = "SELECT * FROM organization_task WHERE organization_name = ?";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setString(1, this.name);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            int taskId = rs.getInt("task_id");
            String assignee = rs.getString("assignee");
            String description = rs.getString("task_description");
            Date assigningDate = rs.getDate("assigning_date");
            Date deadline = rs.getDate("deadline_date");
            String PriorityStr = rs.getString("TaskPriority");
            TaskPriority priority = TaskPriority.valueOf(PriorityStr);
            
            Task task = new Task(taskId, description, assigningDate, deadline, priority);
            tasks.get(assignee).add(task);
        }
    }

    // Method to add member 
    public void addMember(String username, String role) throws Exception {
        roles.put(username, role);
        tasks.put(username, new ArrayList<>());

        Connection con = DatabaseConnectionManager.getConnection();
        String sql = "INSERT INTO organization (organization_name, username, role) VALUES (?, ?, ?)";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setString(1, this.name);
        pst.setString(2, username);
        pst.setString(3, role);
        pst.executeUpdate();
    }
    
    // adding task to a particualr member 
    public void addTask(String assignee, Task task) throws Exception {
        tasks.get(assignee).add(task);
        String deadlineStr = dateFormat.format(task.getDeadline());
        Date todayDate = new Date();
        String assigneeDate = dateFormat.format(todayDate);
        Connection con = DatabaseConnectionManager.getConnection();
        String sql = "INSERT INTO organization_task (organization_name, assignee, task_description, assigning_date, deadline_date, status, TaskPriority) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setString(1, this.name);
        pst.setString(2, assignee);
        pst.setString(3, task.getDescription());
        pst.setDate(4, java.sql.Date.valueOf(assigneeDate));
        pst.setDate(5, java.sql.Date.valueOf(deadlineStr));
        pst.setString(6, task.getStatus().toString());
        pst.setString(7, task.getPriority().toString());
        pst.executeUpdate();
    }

    // removing task from particular member 
    public void removeTask(String assignee, Task task) throws Exception {
        tasks.get(assignee).remove(task);

        Connection con = DatabaseConnectionManager.getConnection();
        String sql = "DELETE FROM organization_task WHERE task_id = ?";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, task.getTask_id());
        pst.executeUpdate();
    }

    // marking a task as completed or not 
    public void markTaskCompleted(String assignee, Task task) throws Exception {
        task.markCompleted();

        Connection con = DatabaseConnectionManager.getConnection();
        String sql = "UPDATE organization_task SET status = 'COMPLETED' WHERE task_id = ?";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, task.getTask_id());
        pst.executeUpdate();
    }

    // Task Priority of the organizational
    public void TaskPriority(String task_description)throws Exception{
        System.out.println("Enter the value of priority of the task(HIGH,MEDIUM,LOW)");
        String priorityStr = scanner.nextLine();
        Connection con = DatabaseConnectionManager.getConnection();
        if (con!= null) {
            String sql = "Update organizational SET TaskPriority = ? WHERE task_description = ? ";
            PreparedStatement pst = con.prepareCall(sql);
            pst.setString(1,priorityStr);
            pst.setString(2,task_description);
            pst.executeUpdate();
            con.close();
        }
    }

    // method to get the task detail
    public List<Task> getTasks(String assignee) {
        return tasks.get(assignee);
    }

    @Override
    public String toString() {
        return "Organization: " + name + ", Roles: " + roles;
    }
}

//Class for the Database connectivity.
class DatabaseConnectionManager {
    public static Connection connection;

    public static Connection getConnection() throws Exception {
        if (connection == null) {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/taskmanagementdatabase", "root", "");
        }
        return connection;
    }
}

// Main class to demonstrate functionality
public class TaskManagementSystem {
    static Scanner scanner = new Scanner(System.in);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    //main method to operate all methods 
    public static void main(String[] args) throws Exception {

        Connection con = DatabaseConnectionManager.getConnection();
        if (con != null) {
            System.out.println("The connection is established successfully.");
        }
        System.out.println("Wait till we fetch your data.");
        Thread.sleep(1000);
        List<Task> fetched_task = fetchingData();
        Thread.sleep(500);
        System.out.println("Welcome to Task Management System!");
        System.out.println("Choose an option:");
        System.out.println("1. Personal Usage");
        System.out.println("2. Organizational Usage");
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1 -> // Personal Usage
                personalTaskManagement(fetched_task);
            case 2 -> // Organizational Usage
                organizationalTaskManagement();
            default -> System.out.println("Invalid choice. Exiting...");
        }
    }

    //Method of fetching the data from the personal_task table 
    static List<Task> fetchingData() throws Exception {
        Connection con = DatabaseConnectionManager.getConnection();
        String sql = "SELECT * FROM personal_task";
        PreparedStatement pst = con.prepareCall(sql);
        ResultSet rs = pst.executeQuery();
        List<Task> fetched_task = new ArrayList<>();
        while (rs.next()) {
            int task_id = rs.getInt("task_id");
            String task_description = rs.getString("task_description");
            Date assigning_date = rs.getDate("assigning_date");
            Date deadline_date = rs.getDate("deadline_date");
            String PriorityStr = rs.getString("TaskPriority");
            TaskPriority priority = TaskPriority.valueOf(PriorityStr);
            fetched_task.add(new Task(task_id, task_description, assigning_date, deadline_date, priority));
        }
        if (!rs.next()) {
            System.out.println("The previous data has been fetched.");
        }
        return fetched_task;
    }

    //Method to run the personal task management system.
    private static void personalTaskManagement(List<Task> fetched_Tasks) throws Exception {
        List<Task> tasks = new ArrayList<>();
        tasks.addAll(fetched_Tasks);
        Connection con = null;
        while (true) {
            System.out.println("\nPersonal Task Management Menu:");
            System.out.println("1. Add Task");
            System.out.println("2. Remove Task");
            System.out.println("3. Mark Task as Completed");
            System.out.println("4. List Tasks");
            System.out.println("5. reset priority");
            System.out.println("6. Exit");
            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); 

            switch (choice) {
                case 1 -> {
                    // Adding a task
                    System.out.print("Enter task description: ");
                    String description = scanner.nextLine();
                    System.out.print("Enter deadline (yyyy-MM-dd): ");
                    String deadlineStr = scanner.nextLine();
                    Date todayDate = new Date();
                    String assigningDate = dateFormat.format(todayDate);
                    System.out.println("Enter the task_id");
                    int task_id = scanner.nextInt();
                    scanner.nextLine();
                    try {
                        Date deadline = dateFormat.parse(deadlineStr);
                        if(deadline.compareTo(todayDate)>=0){
                            System.out.println("Enter the task priority");
                            String priorityStr = scanner.nextLine();
                            TaskPriority priority = TaskPriority.valueOf(priorityStr);
                            tasks.add(new Task(task_id, description, todayDate, deadline,priority));
                            con = DatabaseConnectionManager.getConnection();
                            String sql = "INSERT INTO personal_task (Task_id, Task_description, Assigning_date, Deadline_date) VALUES (?, ?, ?, ?)";
                            PreparedStatement pst = con.prepareStatement(sql);
                            pst.setInt(1, task_id);
                            pst.setString(2, description);
                            pst.setDate(3, java.sql.Date.valueOf(assigningDate));
                            pst.setDate(4,  java.sql.Date.valueOf(deadlineStr));
                            int rowsEffected = pst.executeUpdate();
                            if (rowsEffected > 0) {
                                System.out.println("The task has been added successfully.");
                            }
                        }else{
                            System.out.println("invalid date");
                        }
                    } catch (Exception e) {
                        System.out.println("Something went wrong please try again.");
                    }
                }
                case 2 -> {
                    // Removing task
                    System.out.print("Enter the task description to remove: ");
                    String removeDescription = scanner.nextLine();
                    boolean removed = false;
                    for (Task task : tasks) {
                        if (task.getDescription().equalsIgnoreCase(removeDescription)) {
                            task.removeCompletely(removeDescription);
                            removed = tasks.remove(task);
                            break;
                        }
                    }
                    if (removed) {
                        System.out.println("Task removed successfully.");
                    } else {
                        System.out.println("Task not found.");
                    }
                }
                case 3 -> {
                    // Marking task as complete
                    System.out.print("Enter the task description to mark as completed: ");
                    String completeDescription = scanner.nextLine();
                    boolean marked = false;
                    for (Task task : tasks) {
                        if (task.getDescription().equalsIgnoreCase(completeDescription)) {
                            task.markCompleted();
                            System.out.println("Task marked as completed.");
                            marked = true;
                            String reset_status = "COMPLETED";
                            if (con!= null) {
                                String sql = "update personal_task set status = ? where task_description = ?";
                                PreparedStatement pst = con.prepareCall(sql);
                                pst.setString(1,reset_status);
                                pst.setString(2,completeDescription);
                            }
                            break;
                        }
                    }
                    if (!marked) {
                        System.out.println("Task not found.");
                    }
                }
                case 4 -> {
                    // Listing task
                    System.out.println("Listing tasks:");
                    for (Task task : tasks) {
                        System.out.println(task);
                    }
                }
                case 5 -> {
                    // changing the priority of Task.
                    System.out.println("Enter task description of the task you want to set priority of.");
                    String task_descriptioString = scanner.nextLine();
                    boolean reset = true;
                    for (Task task : tasks) {
                        if (task.getDescription().equalsIgnoreCase(task_descriptioString)) {
                            task.TaskPriority(task_descriptioString);
                            System.out.println("Task priority reset succesfully.");
                            reset = false;
                            break;
                        }
                    }
                    if (reset) {
                        System.out.println("Task not found");
                    }
                }
                case 6 -> {
                    // Exiting Personal task management system
                    System.out.println("Exiting Personal Task Management...");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    //Method to run the Organisation Task management system
    private static void organizationalTaskManagement() throws Exception {
        try (Scanner sc = new Scanner(System.in)) {
            int choice1;
            do{
                System.out.println("1. Create organisation.");
                System.out.println("2. Enter into existing organisation.");
                System.out.println("3. Exit");

                choice1 = scanner.nextInt();

                switch(choice1){
                    case 1: 
                        createOrganization();
                        break;
                    case 2: 
                    System.out.println("Enter the org name ");
                        String organizationName = sc.nextLine();
                        Organization organization = new Organization(organizationName);
                        Connection con = DatabaseConnectionManager.getConnection();
                        String sql = "select organization_name from organization";
                        PreparedStatement pst = con.prepareCall(sql);
                        ResultSet rs = pst.executeQuery();
                        while(rs.next()){
                            String organization_name = rs.getString("organization_name");
                            if(organization_name.equalsIgnoreCase(organizationName)){
                                System.out.println(organizationName);
                                break;
                            }
                        }
                        
                        System.out.println("Enter your designation in the organisation");
                        String designation = sc.nextLine();
                        if(designation.equalsIgnoreCase("Team Leader")){
                            while (true) {
                                System.out.println("\nOrganizational Task Management Menu:");
                                System.out.println("1. Add Member");
                                System.out.println("2. Add Task");
                                System.out.println("3. Remove Task");
                                System.out.println("4. Mark Task as Completed");
                                System.out.println("5. List Tasks");
                                System.out.println("6. Exit");
                                System.out.print("Enter your choice: ");
                                int choice = sc.nextInt();
                                scanner.nextLine();
                    
                                switch (choice) {
                                    case 1 -> {
                                        // Enlist the member ny entering their name
                                        System.out.print("Enter member username: ");
                                        String username = scanner.nextLine();
                                        System.out.print("Enter member role: ");
                                        String role = scanner.nextLine();
                                        organization.addMember(username, role);
                                        System.out.println("Member added successfully.");
                                    }
                                    case 2 -> {
                                        // Adding a task to particular assignee(Member)
                                        System.out.print("Enter assignee username: ");
                                        String assignee = scanner.nextLine();
                                        System.out.print("Enter task description: ");
                                        String description = scanner.nextLine();
                                        System.out.print("Enter deadline (yyyy-MM-dd): ");
                                        String deadlineStr = scanner.nextLine();
                                        try {
                                            Date deadline = dateFormat.parse(deadlineStr);
                                            Date todayDate = new Date();
                                            if(deadline.compareTo(todayDate)>=0){
                                                System.out.println("Enter the task priority");
                                                String priorityStr = scanner.nextLine();
                                                TaskPriority priority = TaskPriority.valueOf(priorityStr);
                                                Task task = new Task(0, description, todayDate, deadline, priority);
                                                organization.addTask(assignee, task);
                                                System.out.println("Task added successfully.");
                                            }else{
                                                System.out.println("Invalid date");
                                            }
                                        } catch (NullPointerException e) {
                                            System.out.println("Make sure that the organisation includes the member you are assigning the data to");
                                        }
                                    }
                                    case 3 -> {
                                        // Removing a task from the enlisted task of a particualar member
                                        System.out.print("Enter assignee username: ");
                                        String assigneeRemove = scanner.nextLine();
                                        System.out.print("Enter the task description to remove: ");
                                        String removeDescription = scanner.nextLine();
                                        List<Task> tasks = organization.getTasks(assigneeRemove);
                                        boolean removed = false;
                                        for (Task task : tasks) {
                                            if (task.getDescription().equalsIgnoreCase(removeDescription)) {
                                                organization.removeTask(assigneeRemove, task);
                                                removed = true;
                                                break;
                                            }
                                        }
                                        if (removed) {
                                            System.out.println("Task removed successfully.");
                                        } else {
                                            System.out.println("Task not found.");
                                        }
                                    }
                                    case 4 -> {
                                        // marking the task as completed or todo
                                        System.out.print("Enter assignee username: ");
                                        String assigneeComplete = scanner.nextLine();
                                        System.out.print("Enter the task description to mark as completed: ");
                                        String completeDescription = scanner.nextLine();
                                        List<Task> taskList = organization.getTasks(assigneeComplete);
                                        boolean marked = false;
                                        for (Task task : taskList) {
                                            if (task.getDescription().equalsIgnoreCase(completeDescription)) {
                                                organization.markTaskCompleted(assigneeComplete, task);
                                                marked = true;
                                                System.out.println("Task marked as completed.");
                                                break;
                                            }
                                        }
                                        if (!marked) {
                                            System.out.println("Task not found.");
                                        }
                                    }
                                    case 5 -> {
                                        // Enlisting the tasks of any assginee
                                        System.out.print("Enter assignee username to list tasks: ");
                                        String assigneeList = scanner.nextLine();
                                        List<Task> taskListToDisplay = organization.getTasks(assigneeList);
                                        if (taskListToDisplay != null) {
                                            System.out.println("Listing tasks for " + assigneeList + ":");
                                            for (Task task : taskListToDisplay) {
                                                System.out.println(task);
                                            }
                                        } else {
                                            System.out.println("No tasks found for " + assigneeList);
                                        }
                                    }
                                    case 6 -> {
                                        // reset the priority of the task
                                        System.out.print("Enter assignee username: ");
                                        String assignee_name = scanner.nextLine();
                                        System.out.print("Enter the task description to mark as completed: ");
                                        String Task_descriptioString = scanner.nextLine();
                                        List<Task> TaskList = organization.getTasks(assignee_name);
                                        boolean flag = false;
                                        for (Task task : TaskList) {
                                            if (task.getDescription().equalsIgnoreCase(Task_descriptioString)) {
                                                organization.TaskPriority(Task_descriptioString);
                                                flag = true;
                                                System.out.println("Task marked as completed.");
                                                break;
                                            }
                                        }
                                        if (!flag) {
                                            System.out.println("Task not found.");
                                        }
                                    }
                                    case 7 -> {
                                        // Exiting the organisation task management system
                                        System.out.println("Exiting Organizational Task Management...");
                                        return;
                                    }
                                    default -> System.out.println("Invalid choice. Please try again.");
                                }
                            }
                        }else if(designation.equalsIgnoreCase("Executive Head")){
                            while (true) {
                                System.out.println("\nOrganizational Task Management Menu:");
                                System.out.println("1. Mark Task as Completed");
                                System.out.println("2. List Tasks");
                                System.out.println("3. reset task priority");
                                System.out.println("4. Exit ");
                                System.out.print("Enter your choice: ");
                                int choice = scanner.nextInt();
                                scanner.nextLine();
                    
                                switch (choice) {
                                    
                                    case 1 -> {
                                        // marking the task as completed or todo
                                        System.out.print("Enter assignee username: ");
                                        String assigneeComplete = scanner.nextLine();
                                        System.out.print("Enter the task description to mark as completed: ");
                                        String completeDescription = scanner.nextLine();
                                        List<Task> taskList = organization.getTasks(assigneeComplete);
                                        boolean marked = false;
                                        for (Task task : taskList) {
                                            if (task.getDescription().equalsIgnoreCase(completeDescription)) {
                                                organization.markTaskCompleted(assigneeComplete, task);
                                                marked = true;
                                                System.out.println("Task marked as completed.");
                                                break;
                                            }
                                        }
                                        if (!marked) {
                                            System.out.println("Task not found.");
                                        }
                                    }
                                    case 2 -> {
                                        // Enlisting the tasks of any assginee
                                        System.out.print("Enter assignee username to list tasks: ");
                                        String assigneeList = scanner.nextLine();
                                        List<Task> taskListToDisplay = organization.getTasks(assigneeList);
                                        if (taskListToDisplay != null) {
                                            System.out.println("Listing tasks for " + assigneeList + ":");
                                            for (Task task : taskListToDisplay) {
                                                System.out.println(task);
                                            }
                                        } else {
                                            System.out.println("No tasks found for " + assigneeList);
                                        }
                                    }
                                    case 3 -> {
                                        // reset the priority of the task
                                        System.out.print("Enter assignee username: ");
                                        String assignee_name = scanner.nextLine();
                                        System.out.print("Enter the task description to mark as completed: ");
                                        String Task_descriptioString = scanner.nextLine();
                                        List<Task> TaskList = organization.getTasks(assignee_name);
                                        boolean flag = false;
                                        for (Task task : TaskList) {
                                            if (task.getDescription().equalsIgnoreCase(Task_descriptioString)) {
                                                organization.TaskPriority(Task_descriptioString);
                                                flag = true;
                                                System.out.println("Task marked as completed.");
                                                break;
                                            }
                                        }
                                        if (!flag) {
                                            System.out.println("Task not found.");
                                        }
                                    }
                                    case 4 -> {
                                        // Exiting the organisation task management system
                                        System.out.println("Exiting Organizational Task Management...");
                                        return;
                                    }
                                    default -> System.out.println("Invalid choice. Please try again.");
                                }
                            }
                        }else if(designation.equalsIgnoreCase("member")){
                            System.out.println("1. List Tasks");
                            System.out.println("2. reset task priority");
                            System.out.println("3. Exit ");
                            System.out.print("Enter your choice: ");
                            int choice = scanner.nextInt();
                            scanner.nextLine();
                
                            switch (choice) {
                                
                                case 1 -> {
                                    // Enlisting the tasks of any assginee
                                    System.out.print("Enter assignee username to list tasks: ");
                                    String assigneeList = scanner.nextLine();
                                    List<Task> taskListToDisplay = organization.getTasks(assigneeList);
                                    if (taskListToDisplay != null) {
                                        System.out.println("Listing tasks for " + assigneeList + ":");
                                        for (Task task : taskListToDisplay) {
                                            System.out.println(task);
                                        }
                                    } else {
                                        System.out.println("No tasks found for " + assigneeList);
                                    }
                        }
                                case 2 -> {
                                    // reset the priority of the task
                                    System.out.print("Enter assignee username: ");
                                    String assignee_name = scanner.nextLine();
                                    System.out.print("Enter the task description to mark as completed: ");
                                    String Task_descriptioString = scanner.nextLine();
                                    List<Task> TaskList = organization.getTasks(assignee_name);
                                    boolean flag = false;
                                    for (Task task : TaskList) {
                                        if (task.getDescription().equalsIgnoreCase(Task_descriptioString)) {
                                            organization.TaskPriority(Task_descriptioString);
                                            flag = true;
                                            System.out.println("Task marked as completed.");
                                            break;
                                        }
                                    }
                                    if (!flag) {
                                        System.out.println("Task not found.");
                                    }
                        }
                                case 3 -> {
                                    // Exiting the organisation task management system
                                    System.out.println("Exiting Organizational Task Management...");
                                    return;
                        }
                                default -> System.out.println("Invalid choice. Please try again.");
                            }
                        }else{
                            System.out.println("Enter valid designtion");
                        }
                    case 3: 
                    System.exit(0);
                    break;   
                }
            }while(choice1<3);
        }
    }

    public static void createOrganization() throws Exception{
        try (Connection con = DatabaseConnectionManager.getConnection()) {
            try (Scanner sc = new Scanner(System.in)) {
                System.out.print("Enter organization name: ");
                String orgName = sc.nextLine();
                Organization organization = new Organization(orgName);
                
                while (true) {
                    System.out.print("Enter Employee name (or type 'done' to finish) : ");
                    String employeeName = sc.nextLine();
                    if (employeeName.equalsIgnoreCase("done")) {
                        break;
                    }
                    System.out.println("Enter the role.");
                    String role = sc.nextLine();
                    
                    System.out.println(orgName);
                    System.out.println(employeeName);
                    System.out.println(role);
                    String sql = "insert into Organization(organization_name, username, role) values(?,?,?)";
                    PreparedStatement pst = con.prepareCall(sql);
                    pst.setString(1,orgName);
                    pst.setString(2,employeeName);
                    pst.setString(3,role);
                    pst.execute();
                    if(role.equalsIgnoreCase("member")){
                        organization.addMember(employeeName, "member");
                    }
                }
            }
        }
        System.out.println("Organization created successfully!");
    }
}
