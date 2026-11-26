use std::collections::HashMap;
use std::io;
fn main() {
    use crate::functions;
    let mut database: HashMap<String,Vec<String>> = HashMap::new();

    let entries = vec![
    "Add Sally to Engineering",
    "Add Amir to Sales",
    "Add Maya to Marketing",
    "Add Liam to Engineering",
    "Add Chloe to HR",
    "Add Noah to Finance",
    "Add Sofia to Operations",
    "Add Ethan to Sales",
    "Add Ava to Engineering",
    "Add Lucas to Marketing",
    "Add Mia to Support",
    "Add Mason to Finance",
    "Add Amelia to Engineering",
    "Add Logan to Legal",
    "Add Harper to HR",
    "Add James to Operations",
    "Add Evelyn to Sales",
    "Add Benjamin to Support",
    "Add Abigail to Legal",
    "Add Elijah to Engineering",
];

    let val = loop {
        let mut entry = String::new();
        println!("Are you using text entry (type: 'text') or array entry (type: 'array')");
        io::stdin()
            .read_line(&mut entry)
            .expect("Please enter a valid entry");
            
        if entry.trim() == "text" || entry.trim() == "array" {
            break entry.trim().to_string();
        }else {
            println!("Please use either 'text' or 'array' ");
        }
    };

    functions::run(val, &mut database,entries.clone());

    println!("{:#?}", database);
}

mod functions {
    use crate::HashMap;
    use crate::io;
    fn terminal_entry(data: &mut HashMap<String,Vec<String>>) {
        loop {
            let mut entry = String::new();

            println!("Please Enter your Entry as ['Add' Name 'to' Department]");
            io::stdin()
                .read_line(&mut entry)
                .expect("Please enter a valid entry");

            let split: Vec<&str> = entry.trim().split_whitespace().collect();

            if split[0] != "Add" || split[2] != "to" {
                println!("Please enter a valid entry for example ['Add' Name 'to' Department]");
                continue;
            }

            let en = data.entry(split[3].to_string()).or_insert(Vec::new());
            en.push(split[1].to_string());

            let ans = loop {
                let mut request = String::new();

                println!("Are there any more entries ( y: yes | q: quit )");

                io::stdin()
                    .read_line(&mut request)
                    .expect("Enter a valid string");

                if request.trim() == "y" || request.trim() == "q" {
                    break request.trim().to_string();
                }else {
                    println!("Please enter ( y: yes | q: quit )");
                }
            };

            if ans == "q" {
                break
            }
        }
    }

    fn array_entry(data: &mut HashMap<String,Vec<String>>, ls: Vec<&str>) {
        let mut invalid: Vec<&str> = Vec::new();
        for ele in ls {
            let split: Vec<&str> = ele.split_whitespace().collect(); 
            if split[0] != "Add" || split[2] != "to" {
                invalid.push(ele);
                continue;
            }

            let en = data.entry(split[3].to_string()).or_insert(Vec::new());
            en.push(split[1].to_string());

        }

        if invalid.len() > 0 {
            println!("The following were invalid entries and were not added in the database");
        }
        for i in invalid {
            println!("{}--is invalid!!", i);
        }
    }

    pub fn run(s: String, data: &mut HashMap<String,Vec<String>>, entries: Vec<&str>) {
        if s == "text" {
            terminal_entry(data);
        }else {
            array_entry(data, entries);
        }
    }
}
