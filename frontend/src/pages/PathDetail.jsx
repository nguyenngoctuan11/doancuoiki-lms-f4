import React, { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { API_BASE_URL } from "../api/httpClient";

export default function PathDetail(){
  const { id } = useParams();
  const API = API_BASE_URL;
  const [path, setPath] = useState({ items: [] });
  useEffect(()=>{
    fetch(`${API}/api/paths/${id}`).then(r=>r.json()).then(setPath).catch(()=>setPath({items:[]}));
  }, [API, id]);
  return (
    <div className="max-w-5xl mx-auto p-6">
      <h1 className="text-2xl font-semibold text-stone-900">{path.name || 'Lộ trình'}</h1>
      <div className="mt-6 grid sm:grid-cols-2 gap-5">
        {path.items.map((c, idx) => (
          <Link key={c.courseId} to={`/learn/${c.slug}`} className="group border border-stone-200 rounded-xl overflow-hidden hover:shadow">
            {c.thumbnailUrl ? (
              <img src={c.thumbnailUrl.startsWith('/')? `${API}${c.thumbnailUrl}`: c.thumbnailUrl} alt={c.title} className="aspect-video w-full object-cover" />
            ) : (
              <div className="aspect-video bg-stone-100" />
            )}
            <div className="p-4">
              <div className="text-xs text-stone-500">Bước {idx+1} • {c.level}</div>
              <div className="font-semibold text-stone-900 group-hover:text-primary-700">{c.title}</div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}

