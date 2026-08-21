with Ada.Strings.Fixed;
with Ada.Strings.Unbounded;
with Ada.Text_IO;
with Words_Engine.Roman_Numerals_Package;

procedure Roman_Oracle is
   use Ada.Strings.Unbounded;

   Line  : Unbounded_String;
   Value : Natural;
begin
   loop
      begin
         Line := To_Unbounded_String (Ada.Text_IO.Get_Line);
         Value := Words_Engine.Roman_Numerals_Package.Roman_Number
           (To_String (Line));
         Ada.Text_IO.Put_Line
           (To_String (Line) & ASCII.HT &
            Ada.Strings.Fixed.Trim (Natural'Image (Value), Ada.Strings.Both));
      exception
         when Ada.Text_IO.End_Error =>
            exit;
      end;
   end loop;
end Roman_Oracle;
