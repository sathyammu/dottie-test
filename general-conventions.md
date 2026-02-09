 - use Lombok for class Generation
 - Favour records over Java Classes. 
 - Add @Service, @Slf4J, @RequiredArgsConstructor for Services. etc. .
 - In the diff context that you send for Search Replace tags, include a context of min 3 non-empty lines 
    so that the diff is correctly resolved. 
 - When there is an exception use the following to wrap the excpetion
  ```
   public interface ThrowingSupplier<T> extends Supplier<Optional<T>> {
   Logger log = LoggerFactory.getLogger(ThrowingSupplier.class);

   static <U> Optional<U> getCapturingExceptions(ThrowingSupplier<U> supplier) {
     //Exception generating code.
   }
```
- Add new flyway migrations to `src/main/resources/db/migration. Create a generic V100__ prefixed file. Use PascalCase for the filename.


# Reading source file 

If you want to know about a package, ask me to open the relevant file. Standard maven semantics for converting package names to source file names

# Task Tree Structure 
The Task / TaskPretty entities have a tree structure :

select sequence, input_json #>> '{parent}' as parent from task_pretty
"sequence"	"parent"
13215	S.19689
13216	S.19689.13215
13219	S.19689.13215.13216
13218	S.19689.13215.13216
13292	S.19689.13215.13216.13217
13291	S.19689.13215.13216.13218


Here the combination of parent + sequence forms the pg's ltree's syntax for tree representation. 

A Task_Tree table also exists that ties these tasks together

select * from task_tree limit 1
"id","qualifier","levels","meta","created_at"
10587,T52xEBYxrR-21mQCgHY9Q,S.8584.5590.6120.__END__,"{""topic"": ""INTERNAL_SLEEP""}",2025-03-22 23:22:38.494 +0000

The levels column contains the ltree syntax for parent + sequence as seen above.  

Each task has a topic, which denotes what it does. Each topic can be varied rom downloading a file, performing extensive analysis etc. 
Read com.brimmatech.general.config.TemplateConfig.TOPICS for all the topics on the system. Each topic is implemented by a delegate class, which is mapped at com.brimmatech.general.config.TaskConfig.processTask. Each delegate process the task based on its Input and Output parameters which is denoted by the fields com.brimmatech.docflow.v2.task.delegates.TaskDelegate.inputArgsType and com.brimmatech.docflow.v2.task.delegates.TaskDelegate.outputArgsType on that class. Input types are defined in com.brimmatech.docflow.v2.task.dto.CreationArgs and saved in the task as com.brimmatech.docflow.v2.models.TaskPretty.inputJson. Output ares are defined in com.brimmatech.docflow.v2.task.dto.TaskOutputArgs. 

To read the available task topics

# UI Conventions 
When asked to implement a UI feature, choose htmx, Controllers, bootstrap etc. Ask for references in templates/admin/layout.html and other related files. Maintain existing conventions for UI controllers that serve htmx fragments.   

# Tenant Settings 