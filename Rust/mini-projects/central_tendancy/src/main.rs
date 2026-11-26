fn main() {
    use crate::functions;
    let data = [61, 24, 81, 70, 30, 45, 96, 84, 84, 97, 33, 12, 
    45, 62, 11, 97, 39, 47, 45, 73, 69, 30, 42, 85, 67, 31, 98, 58, 68, 51]; //Dummy data

    println!("The mean is {}",  functions::mean(&data));
    println!("The mode is {}",  functions::mode(&data));
    println!("The median is {}",functions::median(&data));
}

pub mod functions{
    use std::collections::HashMap;

    pub fn mean (d: &[isize]) -> isize {
        let mut sum = 0;
        for ele in d {
            sum += ele;
        }
        sum / (d.len() as isize)
    }


    pub fn mode (d: &[isize]) -> isize {
        let mut map = HashMap::new();

        for ele in d {
            let cnt = map.entry(ele).or_insert(0);
            *cnt += 1;
        }

        let keys: Vec<&isize> = map.keys().copied().collect();

        let max_value = match keys.iter().max() {
            Some(&max) => max,
            _ => &-1,
        };

        *max_value
    }


    pub fn median (d: &[isize]) -> isize {
        
        let mut vec: Vec<isize> = Vec::from(d);

        vec.sort();

        let  mid = vec.len() / 2;

        if mid % 2 != 0 {
            (vec[mid - 1] + vec[mid + 1]) / 2
        }else {
            vec[mid]
        }
    }
}



