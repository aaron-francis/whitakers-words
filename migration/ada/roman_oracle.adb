--  Reference implementation of the oracle contract in migration/CONTRACT.md.
--
--  Reads candidate inputs from stdin, one per line, and writes one line per
--  input to stdout as <input><TAB><result>, where <result> is the value
--  returned by the unmodified legacy Words_Engine.Roman_Numerals_Package
--  .Roman_Number.

with Ada.Text_IO;
with Words_Engine.Roman_Numerals_Package;

procedure Roman_Oracle is
   use Ada.Text_IO;
begin
   while not End_Of_File loop
      declare
         Line  : constant String  := Get_Line;
         Value : constant Natural :=
           Words_Engine.Roman_Numerals_Package.Roman_Number (Line);
      begin
         Put_Line (Line & Character'Val (9) & Natural'Image (Value)
           (2 .. Natural'Image (Value)'Last));
      end;
   end loop;
end Roman_Oracle;
