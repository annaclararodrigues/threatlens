import style from "./AlertSelectedTags.module.css";

export default function AlertSelectedTags({name, onClose, editing}) {
    return (
        <div className={style.tag}>
            <p>{name}</p>
            {editing && 
                <div className={style.closeIcon} onClick={onClose}>
                    ✖
                </div>
            }
            
        </div>
    );
}