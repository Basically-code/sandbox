fn main() {
    use crate::translator;
    let data: Vec<&str> = vec![
        "the quick brown fox jumps over the lazy dog",
        "apple trees grow sweet fruit in warm spring sunlight",
        "bright stars shine across the quiet midnight sky"
    ];
    let results: Vec<&str> = vec![
        "ethay uickqay ownbray oxfay umpsjay overway ethay azylay ogday",
        "appleway eestray owgray eetsway uitfray inway armway ingspray unlightsay",
        "ightbray arsstay ineshay acrossway ethay uietqay idnightmay yskay"
    ];
    
    let mut end: Vec<String> = Vec::new();
    
    for ele in data {

        let mut temp = String::new();

        let split: Vec<&str> = ele.split_whitespace().collect();

        for i in 0..split.len() {
            let t = translator::switch(split[i]);
            if i == 0 {
                temp.push_str(&t.to_string());
            }else {
                temp.push_str(&format!(" {}",&t));
            }
        }
        end.push(temp)
    }
    for i in 0..end.len() {
        println!("Test {}: {}",i, end[i] == (results[i]));
    }
}


mod translator {
    fn consonant(s: &str) -> String {
        let c: Vec<char> = s.chars().collect();
        let mut n = 0;
        for uc in c {
            if vowel_checker(uc) {
                break;
            }
            n += 1
        }
        if n == s.len() {n -= 1}
        let st = s.to_string();
        format!("{}{}ay",&st[n..st.len()],&st[0..n])
    }

    fn vowel(s: &str) -> String {
        format!("{}way",s)
    }

    pub fn switch(s: &str) -> String {
        let c: Vec<char> = s.chars().collect();
        if vowel_checker(c[0]) {
            vowel(s)
        }else {
            consonant(s)
        }
    }

    fn vowel_checker(s: char) -> bool {
        let v = ['a','e','i','o','u'];
        for ele in v {
            if ele == s {
                return true
            }
        }
        false
    }
}
