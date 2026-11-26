// Find two indexes in an array that sum up to the result and return it as a tuple if non return (0,0)
use std::collections::HashMap;
fn main() {}

    /* For testing purposes
    let n = [-1,3,2,4,10];
    let target = 9;
    */

    pub fn finder(list: &[i32], target: i32) -> (usize,usize) {
        let mut map: HashMap<i32,usize> = HashMap::new();

        for (i,n) in list.iter().enumerate() {
            if map.contains_key(&(target - n)) {
                return (map[&(target - n)], i);
            }else {
                map.insert(*n,i);
            }
        }
        (0,0)
    }


    #[cfg(test)]
    mod tests {
        use crate::finder;
        #[test]
        fn functionality_check(){
            assert_eq!(finder(&[20,100,-20,10], 0), (0,2));
            assert_eq!(finder(&[90,82,17,30], 3), (0,0));
        }

        #[test]
        #[should_panic]
        fn functionality_check2(){
            assert_eq!(finder(&['a','b'], 5),(0,1));
        }
    }



