package sg.edu.nus.wing.qanus.mitic.first_step;

public enum QCEnumTypes
{
    NOVALUE,                         //Error
    HUM, DESC, ENTY, LOC, NUM, ABBR,  //Classes 
    WHO, WHOM, WHERE, WHICH, //asked entity
    ind,gr,title,desc,               //HUM subclasses
    state,other,city,country,mount,  //LOC subclasses
    exp,abb,                         //ABBR subclasses
    manner,reason,def, //desc,          //DESC subclasses    
    substance,sport,plant,techmeth,cremat,animal,event,letter,religion,food,product,color,termeq,body,dismed,instru,word,lang,symbol,veh,currency, //other,//ENTY subclasses
    date,count,money,period,volsize,speed,perc,code,dist,temp,ord,weight; //other,//NUM subclasses
    
    
    public static QCEnumTypes val(String str)
    {
        try {
            return valueOf(str);
        } 
        catch (IllegalArgumentException ex) {
            //System.out.println("Error en Switchy: "+str);
            return NOVALUE;
        }
    }    
}