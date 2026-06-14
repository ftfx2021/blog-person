
import { useEffect, useState } from "react";
function HelloWorld() {
    const [count,setCount] = useState(0)
    useEffect(()=>{
        console.log("组件已经挂载")
        return ()=>{
            console.log("组件将卸载")
        }
        
    },[])


    useEffect(()=>{
        console.log('count变化',count)
    },[count])


    useEffect(()=>{
        console.log("组件已经更新")
    })



  

  const [day,setDay] = useState(0)
  const [user,setUser]=useState({
    name:"张三",
    age:25
  })
  function addDay(){
    setDay(day+1)
    console.log("今天是学习react的第"+day+"天")
}
    function addAge(){
        setUser(prev=>({
            ...prev,
            age:prev.age+1
        }))
    }
  return (
    <div>
      <h1>Hello, World!</h1>
      <div>今天是学习react的第{day}天</div>
      <p>这是我的第一个React组件</p>
      <p>{user.name}的年龄是{user.age}</p>
      <button type = "button" style={{color:'red',fontSize:'16px'}} onClick={addDay}> 日期加一</button>

            <button type = "button" style={{color:'blue',fontSize:'16px'}} onClick={addAge}> 年龄加一</button>
            <button onClick={() => setCount(c => c + 1)}>{count}</button>
    </div>
  )
  
}
var i = 0;



export default HelloWorld