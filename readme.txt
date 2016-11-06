Automatic PL/SQL Code Checks and Edits
(just testing the CodeWalker class in plsql-parser)


class spinat.plsqlchecks.Varchar2Fixer

This program fixes declarations of kind varchar2(23) to varchar2(23 char).
Of course char() and varchar() are also handled.

Call 
dir <directoryname>

fixes all files in dirextory and replaces the files in place.
You are using source control?

Given a file name, determine what the contents are: spec or body and then
retrive all varchar() declarations. 
These can occur as 
  variable type,
  record field,
  table of something

Of course it would be simpler to do this with a regular expression.
But this is a test for the pl/sql parser.

class spinat.plsqlchecks.WhenOthersThenBad

works on a directory

complains about exception handlers like:

when others then null;

just reraising then exception
when others then raise;


just logging and then raise ...
when others then
  logging.log_info(...)  (or something similar)
  ...
  raise;  
end;
