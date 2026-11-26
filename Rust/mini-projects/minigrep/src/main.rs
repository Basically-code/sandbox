use std::env;
use std::fs;
use std::process;
use std::error::Error;

fn main() {
    let args: Vec<String> = env::args().collect();
    
    let config = Config::build(&args).unwrap_or_else(|err|{
        println!("There was a problem: {err}");
        process::exit(1);
    });

    println!("\nSearching for {}",config.query);
    println!("In file {}",config.file_path);
    
    if let Err(e) = run(config) {
        println!("Application error: {e}");
        process::exit(1);
    }

}

struct Config {
    query: String,
    file_path: String,
}


impl Config {
    fn build(args: &Vec<String>) -> Result<Config, &'static str> {
        if args.len() < 3 {
            return Err("Not enough Arguments for use");
        }
        Ok(Config {
            query: args[1].clone(),
            file_path: args[2].clone(),
        })
    }
}

fn run(config: Config) -> Result<(), Box<dyn Error>>{
    let contents = fs::read_to_string(config.file_path)?;

    println!("With text:\n{contents}");

    Ok(())
}
